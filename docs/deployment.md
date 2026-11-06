# EC2 continuous deployment

Pushes to main run CI, ensure the required component images exist in GHCR, then
SSH to EC2 as ubuntu and invoke the installed script as root using non-interactive
`sudo -n -i -- bash`. Pull requests only run CI.

## GitHub environment

Create an environment named `ec2` in repository Settings > Environments.

| Type | Name | Value |
| --- | --- | --- |
| Secret | EC2_SSH_PRIVATE_KEY | Entire contents of Matriz_KP.pem, preserving header/footer and line breaks |
| Secret | EC2_SSH_KNOWN_HOSTS | Verified OpenSSH known_hosts entries for the EC2 host |
| Variable | EC2_HOST | EC2 public IPv4 address or DNS name |
| Variable | EC2_USER | Defaults to ubuntu |
| Variable | EC2_PORT | Defaults to 22 |
| Variable | EC2_DEPLOY_SCRIPT | Defaults to /root/deploy-ec2.sh |

The private key secret contains file contents, not its filename. On Windows, copy
the file contents to the clipboard without printing the key:

```powershell
Get-Content -Raw .\Matriz_KP.pem | Set-Clipboard
```

Paste into the environment secret EC2_SSH_PRIVATE_KEY. The workflow writes a
temporary private-key file with restrictive permissions and removes it afterward.
The key must work without a passphrase prompt; this workflow does not configure
an SSH agent for encrypted keys. Never commit the PEM file.

When you answer yes to SSH on your computer, SSH saves the server public key in
your local known_hosts file. Each GitHub runner starts fresh, so it needs that
entry supplied explicitly. After verifying the server fingerprint through a
trusted connection, extract the entry on the same machine/client you used for SSH:

```bash
ssh-keygen -F YOUR_EC2_IP -f ~/.ssh/known_hosts
```

On PowerShell, the equivalent path is `$env:USERPROFILE\.ssh\known_hosts`. Copy
the matching key lines (not comments) into EC2_SSH_KNOWN_HOSTS. Use `[host]:port`
for a nonstandard SSH port. Keep host-key checking enabled; the runner cannot
answer an interactive yes prompt. The security group must permit runner SSH.

GHCR publication uses GITHUB_TOKEN with packages:write. Existing packages must
grant the repository publication access. Public images pull anonymously. For
private packages, log in to GHCR once as root on EC2 with read:packages access,
because root is the account that executes Docker.

## Install on EC2 once

Copy scripts/deploy-ec2.sh to EC2 and place it next to your configured
docker-compose.yml and .env. The default layout is:

- /root/deploy-ec2.sh
- /root/docker-compose.yml
- /root/.env
- any configuration directories referenced by Compose

Alternatively put these in another directory and set EC2_DEPLOY_SCRIPT to the
absolute script path. The script derives its working directory from its own
location; EC2_DEPLOY_DIRECTORY is no longer used. Updating the script in Git does
not update its installed copy; copy the revised script to EC2 when it changes.

The ubuntu account must support passwordless non-interactive sudo. Verify:

```bash
sudo -n -i -- id -u
```

It should print 0. Install Docker Engine, Compose v2, Bash, sed and grep.
Start supporting database/Keycloak services once and verify their health.
Compose must have services named backend and frontend and the same project name
as the running environment (`name: matriz` in the repository file). Keep host
secrets and real database/Keycloak addresses in the host configuration.

## Selective images

Each image is tagged with the full SHA of the latest commit affecting its
component directory, .dockerignore, or the CI workflow. A frontend-only change
therefore keeps the backend tag. Shared image/workflow inputs rebuild both.
The publish matrix checks each tag in GHCR and skips building/publishing when
it exists. Missing tags are built, including on the first run or after a failed
publication. CI tests still run for both components.

Deployment waits for both required images, skips superseded main commits and
serializes runs through Actions concurrency. The installed script validates its
arguments, Compose configuration and the presence of exactly one tagged GHCR
image for each application component. The image names must match
`ghcr.io/<owner>/<repository>-backend:<tag>` and
`ghcr.io/<owner>/<repository>-frontend:<tag>`.

It then runs `docker compose -f docker-compose.yml down`, edits the backend and
frontend tags directly in that file, and runs
`docker compose -f docker-compose.yml up -d`. All services in the Compose project
are stopped and restarted. Named volumes are retained because down does not use
--volumes. Compose pulls missing images during startup.

There are no application health probes, release records or automatic rollback.
Actions still reports command failures, but successful detached startup does not
prove the application is ready. Copy the updated script to EC2 before the next
deployment. The default files are /root/deploy-ec2.sh and /root/docker-compose.yml
(root login home, ~/).

Manual invocation on EC2:

```bash
sudo -n -i -- bash /root/deploy-ec2.sh owner/repository BACKEND_40_CHAR_SHA FRONTEND_40_CHAR_SHA
```

Live deployment requires the environment settings and installed script. Nginx
and HTTPS configuration remain separate from this deployment workflow.
