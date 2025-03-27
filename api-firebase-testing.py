import requests
from io import BytesIO

def identify_plant_from_url(image_url, api_key):
    # Download the image from the URL
    response = requests.get(image_url)
    response.raise_for_status()  # Check if download was successful
    
    # Prepare the form data
    files = {
        'images': ('image.jpg', BytesIO(response.content), 'image/jpeg')
    }
    
    params = {
        'api-key': api_key,
        'include-related-images': 'false',
        'no-reject': 'false',
        'nb-results': 10,
        'lang': 'en',
        'type': 'kt'  # or 'legacy' if needed
    }
    
    # Make the API request
    api_url = "https://my-api.plantnet.org/v2/identify/all"
    response = requests.post(api_url, files=files, params=params)
    
    return response.json()

# Example usage
image_url = ""#testing and this works with firebase images! Yippie!
api_key = ""#removed api key and such
result = identify_plant_from_url(image_url, api_key)
print(result)