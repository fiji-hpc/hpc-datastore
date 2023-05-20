import requests

# Set the base URL for the API
base_url = "http://localhost:8443"

# Test getAllOAuthUsers endpoint
def test_get_all_oauth_users():
    url = base_url + "/oauth-users"
    response = requests.get(url)
    assert response.status_code == 200
    users = response.json()
    assert len(users) > 0
    return len(users)

# Test getUserByAlias endpoint
def test_get_user_by_id(id):
    url = base_url + f"/oauth-users/{id}"
    response = requests.get(url)
    assert response.status_code == 200
    user = response.json()
    assert user["id"] == id

# Test updateOAuthUser endpoint
def test_update_oauth_user(id):
    url = base_url + f"/oauth-users/{id}"
    updated_user = {"clientID": id, "oauthAlias": "upp"}
    response = requests.put(url, json=updated_user)
    assert response.status_code == 204

# Test deleteOAuthUserById endpoint
def test_delete_oauth_user_by_id(id):
    url = base_url + f"/oauth-users/{id}"
    response = requests.delete(url)
    assert response.status_code == 204

# Test createOAuthUser endpoint
def test_create_oauth_user():
    user = {"clientID": "new_user", "oauthAlias": "upp"}
    url = base_url + "/oauth-users"
    response = requests.post(url, json=user)
    assert response.status_code == 201

# Run the tests
test_create_oauth_user()
id=test_get_all_oauth_users()
print(id)
test_get_user_by_id(id)
test_update_oauth_user(id)
test_delete_oauth_user_by_id(id)
