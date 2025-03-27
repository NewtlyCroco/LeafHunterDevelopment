import firebase_admin
from firebase_admin import storage, firestore
from firebase_functions import https_fn
import requests
import os
import json
import urllib.parse
from io import BytesIO

# Initialize Firebase Admin SDK
firebase_admin.initialize_app()
db = firestore.client()

PLANT_NET_API_KEY = os.getenv("PLANT_NET_API_KEY")

@https_fn.on_request()
def get_plant_families(req: https_fn.Request) -> https_fn.Response:
    """Downloads image from Firebase Storage, sends to Pl@ntNet API, and returns plant families."""
    try:
        data = req.get_json()
        image_url = data.get("imageUrl")

        if not image_url:
            return https_fn.Response(json.dumps({"error": "Missing image URL"}), status=400, content_type="application/json")

        parsed_url = urllib.parse.urlparse(image_url)
        if not parsed_url.path.startswith("/v0/b/"):
            return https_fn.Response(json.dumps({"error": "Invalid image URL"}), status=400, content_type="application/json")
        
        bucket_name = parsed_url.path.split("/")[2]  # Extract bucket name
        if bucket_name.endswith(".appspot.com") or bucket_name.endswith(".firebasestorage.app"):
            bucket_name = bucket_name.split(".")[0]  # Remove domain suffix

        path_parts = parsed_url.path.split("/")
        if len(path_parts) > 3 and path_parts[1] == "v0" and path_parts[2] == "b":
            bucket_name = path_parts[3]  # Correctly extract the bucket name
        else:
            return https_fn.Response(json.dumps({"error": "Invalid Firebase Storage URL"}), status=400, content_type="application/json")

        if bucket_name.endswith(".appspot.com") or bucket_name.endswith(".firebasestorage.app"):
            bucket_name = bucket_name.split(".")[0]  # Remove domain suffix

        object_path = parsed_url.path.split("/o/")[1]  # Extract object path
        object_path = urllib.parse.unquote(object_path)  # Decode URL-encoded object path

        bucket = storage.bucket()
        normalized_bucket_name = f"{bucket_name}.firebasestorage.app"  # Normalize the extracted bucket name
        if bucket.name != normalized_bucket_name:
            return https_fn.Response(json.dumps({"error": f"Bucket name mismatch: {bucket.name} != {normalized_bucket_name}"}), status=400, content_type="application/json")

        blob = bucket.blob(object_path)
        image_bytes = BytesIO(blob.download_as_bytes())

        # Send Image to Pl@ntNet API
        api_url = f"https://my-api.plantnet.org/v2/identify/all?api-key={PLANT_NET_API_KEY}"
        files = {'images': ('image.jpg', image_bytes, 'image/jpeg')}
        params = {'nb-results': 1}
        response = requests.post(api_url, files=files, params=params)


        # Plant not reognized
        if response.status_code == 404:
            return https_fn.Response(json.dumps({"error": "Plant not recognized."}), status=404, content_type="application/json")

        if response.status_code != 200:
            return https_fn.Response(json.dumps({"error": "Pl@ntNet API request failed"}), status=500, content_type="application/json")
        
        plant_data = response.json()
        result = plant_data.get("results", [{}])[0]  # Get the first result or an empty dictionary if no results

        if result["score"] < 0.7:
            return https_fn.Response(json.dumps({"error": "Plant not recognized."}), status=404, content_type="application/json")

        species = result.get("species", {})
        genus_name = species.get("genus", {}).get("scientificName", None)
        family_name = species.get("family", {}).get("scientificName", None)
        common_name = species.get("commonNames", [None])[0]  # Get the first common name if available

        # Build the response
        plant_info = {
            "genusName": genus_name,
            "familyName": family_name,
            "commonName": common_name
        }

        return https_fn.Response(json.dumps(plant_info), status=200, content_type="application/json")

    except Exception as e:
        return https_fn.Response(json.dumps({"error": str(e)}), status=500, content_type="application/json")


@https_fn.on_request()
def store_plant_data(req: https_fn.Request) -> https_fn.Response:
    """Stores plant data in Firestore."""
    try:
        data = req.get_json()
        user_id = data.get("userId")
        image_url = data.get("imageUrl")
        lat = data.get("lat")
        lon = data.get("lon")
        plant_data = data.get("plantData")

        if not all([user_id, image_url, lat, lon, plant_data]):
            return https_fn.Response(json.dumps({"error": "Missing required fields"}), status=400, content_type="application/json")

        # Store data in Firestore
        db.collection("users").document(user_id).collection("plants").add({
            "imageUrl": image_url,
            "lat": lat,
            "lon": lon,
            "plantData": plant_data,
            "timestamp": firestore.SERVER_TIMESTAMP
        })

        return https_fn.Response(json.dumps({"message": "Data stored successfully"}), status=200, content_type="application/json")

    except Exception as e:
        return https_fn.Response(json.dumps({"error": str(e)}), status=500, content_type="application/json")

