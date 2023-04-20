import requests
import json

url = 'http://localhost:8443/oauth-users/create'
headers = {'Content-Type': 'application/json'}
data = {
    'OAuthAlias' : 'google',
    'clientToken': 'https://ClientToken.com',
    'clientID': '554143119581-1kmphimgugsrfm45rd65bdja8h3bh27k.apps.googleusercontent.com',
    'clientSecret': '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'
}

response = requests.post(url, headers=headers, data=json.dumps(data))

#print(response.status_code)
if(response.status_code==204):
    print("Vytvořeno OK")
else:
    print("Vytvořeno KO: "+str(response.status_code))