import requests
import json

class OAuthUserNew:
def init(self, ClientToken, ClientID, ClientSecret):
self.ClientToken = ClientToken
self.ClientID = ClientID
self.ClientSecret = ClientSecret

url = 'http://localhost:8443/oauth-users/create'
headers = {'Content-Type': 'application/json'}
data = {
'ClientToken': 'my_token',
'ClientID': 'my_id',
'ClientSecret': 'my_secret'
}

response = requests.post(url, headers=headers, data=json.dumps(data))
if(response.status_code==204):
print("Vytvořeno OK:")
else:
print("Vytvořeno KO:")

id = 'my_id'
updated_user = OAuthUserNew(
'new_token',
'new_id',
'new_secret')

url = f'http://localhost:8443/oauth-users/{id}'
headers = {'Content-type': 'application/json'}

response = requests.put(url, data=json.dumps(updated_user.dict), headers=headers)

if(response.status_code==204):
print("Update OK:")
else:
print("Update KO:")

id = 'my_id'
url = f'http://localhost:8443/oauth-users/{id}'

response = requests.delete(url)

if(response.status_code==204):
print("Delete OK")
else:
print("Delete KO")