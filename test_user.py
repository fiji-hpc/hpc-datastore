import requests
import json

class OAuthUserNew:
    def __init__(self, ClientToken, ClientID, ClientSecret):
        self.ClientToken = ClientToken
        self.ClientID = ClientID
        self.ClientSecret = ClientSecret

url = 'http://localhost:8443/oauth-users/create'
headers = {'Content-Type': 'application/json'}
data = {
    'ClientToken': 'my_client_token',
    'ClientID': 'my_client_id',
    'ClientSecret': 'my_client_secret'
}

response = requests.post(url, headers=headers, data=json.dumps(data))
if response.status_code == 204:
    print("Created OK")
else:
    print("Created KO")

url = 'http://localhost:8443/oauth-users/json'
response = requests.get(url)
data = json.loads(response.text)

id = str(data[-1]['id'])

updated_user = OAuthUserNew(
  'updated_client_token',
  'updated_client_id',
  'updated_client_secret')

url = f'http://localhost:8443/oauth-users/{id}'
headers = {'Content-type': 'application/json'}

response = requests.put(url, data=json.dumps(updated_user.__dict__), headers=headers)

if response.status_code == 204:
    print("Updated OK")
else:
    print("Updated KO")

url = f'http://localhost:8443/oauth-users/{id}'

response = requests.delete(url)

if response.status_code == 204:
    print("Deleted OK")
else:
   print("Deleted KO "+str(response.status_code))
