url="https://$bamboo_deployUsername:$bamboo_deployPassword@$bamboo_jiraBaseUrl/rest/plugins/1.0/"
token=$(curl --globoff -sI "$url" | grep Upm-Token | cut -d: -f2- | tr -d '[[:space:]]')
pluginFile=$(find . -maxdepth 1 -type f -name 'mrimsender-*.jar')
curl "$url?token=$token" --globoff -F plugin=@"$pluginFile"
