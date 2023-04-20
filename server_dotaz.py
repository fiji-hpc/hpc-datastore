import requests
import json

class OAuthServerNew:
    def __init__(self, alias, sub, link_oauth, link_user_info, link_token):
        self.alias = alias
        self.sub = sub
        self.link_oauth = link_oauth
        self.link_user_info = link_user_info
        self.link_token = link_token

url = 'http://localhost:8443/oauth-servers/create'
headers = {'Content-Type': 'application/json'}
data = {
    'alias': 'google',
    'sub': 'my_sub',
    'link_oauth': 'https://accounts.google.com/o/oauth2/v2/auth',
    'link_user_info': 'https://www.googleapis.com/oauth2/v3/userinfo',
    'link_token': 'https://www.googleapis.com/oauth2/v4/token'
}

response = requests.post(url, headers=headers, data=json.dumps(data))
if(response.status_code==204):
    print("Vytvořeno OK:")
else:
    print("Vytvořeno KO:")