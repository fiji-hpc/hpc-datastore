import requests
import json

class OAuthServerNew:
    def __init__(self, alias, sub, link_oauth, link_user_info, link_token):
        self.alias = alias
        self.sub = sub
        self.link_oauth = link_oauth
        self.link_user_info = link_user_info
        self.link_token = link_token

id = '22'  

url = f'http://localhost:8443/oauth-users/{id}'

response = requests.delete(url)

if response.status_code == 204:
    print("Deleted OK")
else:
   print("Deleted KO "+str(response.status_code))