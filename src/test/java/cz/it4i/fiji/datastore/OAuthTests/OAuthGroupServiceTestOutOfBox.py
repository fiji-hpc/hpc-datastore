import requests

# Set the base URL for the API
base_url = "http://localhost:8443"

# Test createOAuthGroup endpoint
def test_create_oauth_group():
    url = base_url + "/oauth-groups"
    response = requests.post(url, json={"name": "Group 1","ownerId":"1"})
    assert response.status_code == 200

# Test getOAuthGroupById endpoint
def test_get_oauth_group_by_id(id):
    url = base_url + f"/oauth-groups/{id}"
    response = requests.get(url)
    assert response.status_code == 200
    group = response.json()
    assert group["id"] == id

# Test getAllOAuthGroups endpoint
def test_get_all_oauth_groups():
    url = base_url + "/oauth-groups"
    response = requests.get(url)
    assert response.status_code == 200
    groups = response.json()
    assert len(groups) > 0
    return len(groups)

# Test updateOAuthGroup endpoint
def test_update_oauth_group(id):
    url = base_url + f"/oauth-groups/{id}"
    updated_group = {"id": id, "name": "Updated Group"}
    response = requests.put(url, json=updated_group)
    assert response.status_code == 200

# Test deleteOAuthGroup endpoint
def test_delete_oauth_group(id):
    url = base_url + f"/oauth-groups/{id}"
    response = requests.delete(url)
    assert response.status_code == 200

# Test addUserToGroup endpoint
def test_add_user_to_group(id):
    user_id = 1
    url = base_url + f"/oauth-groups/{id}/users/{user_id}"
    response = requests.post(url)
    assert response.status_code == 200

# Test removeUserFromGroup endpoint
def test_remove_user_from_group(id):
    user_id = 1
    url = base_url + f"/oauth-groups/{id}/users/{user_id}"
    response = requests.delete(url)
    assert response.status_code == 200

# Test changeGroupPermission endpoint
def test_change_group_permission():
    group_id = 1
    permission_type = "R"
    url = base_url + f"/oauth-groups/{group_id}/setPermission/{permission_type}"
    response = requests.put(url)
    assert response.status_code == 200

# Run the tests
test_create_oauth_group()
id=test_get_all_oauth_groups()
test_get_oauth_group_by_id(id)
test_update_oauth_group(id)
test_delete_oauth_group(id)
test_add_user_to_group(id)
test_remove_user_from_group(id)
test_change_group_permission(id)
