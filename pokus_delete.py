import requests
import json

class OAuthServerNew:
    def __init__(self, alias, sub, link_oauth, link_user_info, link_token):
        self.alias = alias
        self.sub = sub
        self.link_oauth = link_oauth
        self.link_user_info = link_user_info
        self.link_token = link_token

url = 'http://localhost:8443/oauth-servers/json'
response = requests.get(url)

if response.status_code == 200:
    data = json.loads(response.text)
    if len(data) > 0:
        id = str(data[-1]['id'])
        url = f'http://localhost:8443/oauth-servers/{id}'
        response = requests.delete(url)

        if response.status_code == 204:
            print("Delete OK")
        else:
            print(response.status_code)
            print("Delete KO")
    else:
        print("No OAuth servers found.")
else:
    print("Failed to retrieve OAuth servers.")
