import requests
import json

url = 'http://localhost:8443/oauth-servers/create'
headers = {'Content-Type': 'application/json'}
data = {
    'alias': 'google',
    'sub': 'sub',
    'link_oauth': 'https://accounts.google.com/o/oauth2/v2/auth',
    'link_user_info': 'https://www.googleapis.com/oauth2/v3/userinfo',
    'link_token': 'https://www.googleapis.com/oauth2/v4/token'
}

response = requests.post(url, headers=headers, data=json.dumps(data))

print(response.status_code)
if(response.status_code==204):
    print("Vytvořeno OK:")
else:
    print("Vytvořeno KO:")
