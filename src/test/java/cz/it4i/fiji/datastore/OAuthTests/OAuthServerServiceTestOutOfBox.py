import requests

# Set the base URL for the API
base_url = "http://localhost:8443"

# Test getAllOAuthServers endpoint
def test_get_all_oauth_servers():
    url = base_url + "/oauth-servers"
    response = requests.get(url)
    assert response.status_code == 200
    servers = response.json()
    assert len(servers) > 0
    return len(servers)

# Test getServerById endpoint
def test_get_server_by_id(server_id):
    url = base_url + f"/oauth-servers/{server_id}"
    response = requests.get(url)
    assert response.status_code == 200
    server = response.json()
    assert server["id"] == server_id

# Test updateOAuthServer endpoint
def test_update_oauth_server(server_id):
    url = base_url + f"/oauth-servers/{server_id}"
    updated_server = {"id": server_id, "name": "Updated Server"}
    response = requests.put(url, json=updated_server)
    assert response.status_code == 204

# Test deleteOAuthServerById endpoint
def test_delete_oauth_server_by_id(server_id):
    url = base_url + f"/oauth-servers/{server_id}"
    response = requests.delete(url)
    assert response.status_code == 204

# Test createOAuthServer endpoint
def test_create_oauth_server():
    server = {"name": "New Server"}
    url = base_url + "/oauth-servers/create"
    response = requests.post(url, json=server)
    assert response.status_code == 201

# Run the tests
test_create_oauth_server()
id=test_get_all_oauth_servers()
id=id+1
test_update_oauth_server(id)
test_get_server_by_id(id)
test_delete_oauth_server_by_id(id)

