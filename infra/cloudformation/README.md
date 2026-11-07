# Matriz EC2 host

`matriz-ec2.yaml` prepares one Ubuntu x86 host, using AMI
`ami-0fc7005aa213e82fa`, `t3.small` and key pair `Matriz_KP`. It creates and
attaches the `matriz-sg` security group. The root volume is gp3,
8 GiB by default, and is deleted when the instance is terminated. Increase
`RootVolumeSize` if container images/data require more space or the AMI's root
snapshot is larger. No additional data volume is created.

## Create the stack

Use CloudFormation in **eu-west-1 (Ireland)**, upload `matriz-ec2.yaml`, select
`VpcId`, and select a `SubnetId` from that same VPC. The subnet must be public and
have an internet gateway route. The instance receives a public IPv4 address.
The subnet network ACLs must allow the required traffic and outbound package
downloads. The AMI and key pair must exist in that region/account; the supplied
AMI must be Ubuntu 26.04 LTS, x86_64, with root device `/dev/sda1`. These
account-specific properties have not been verified against AWS.

The created security group permits TCP ports 22, 80, 443, 3000, 3001, 5432 and
8080 from all IPv4 (`0.0.0.0/0`) and IPv6 (`::/0`) sources. An IPv6 ingress rule
does not assign an IPv6 address to the instance; the selected VPC/subnet and EC2
network interface would also need IPv6 configuration for direct IPv6 reachability.
Public SSH, PostgreSQL, Grafana and backend access should be narrowed to trusted
source CIDRs before using this beyond a temporary demo.

Alternatively, with AWS CLI configured, replace the subnet placeholder and run
from the repository root:

```powershell
aws cloudformation validate-template --region eu-west-1 --template-body file://infra/cloudformation/matriz-ec2.yaml
aws cloudformation deploy --region eu-west-1 --stack-name matriz-ec2 --template-file infra/cloudformation/matriz-ec2.yaml --parameter-overrides VpcId=vpc-REPLACE_ME SubnetId=subnet-REPLACE_ME
```

## Provisioning and verification

First boot installs Git, Docker Engine (`docker.io`), Compose v2
(`docker-compose-v2`), Node.js and npm from Ubuntu's signed repositories, enabling
Universe if needed. Versions follow Ubuntu's repository updates; no particular
Node.js major version is pinned. Docker is enabled at boot. Run Docker commands
with `sudo`; provisioning does not grant the login user Docker group privileges.

First boot also creates `/root/deploy_ec2.sh` with root-only executable permissions
(`0700`). This is `~/deploy_ec2.sh` after `sudo -i`. Place your configured
`docker-compose.yml` and `.env` beside it in `/root`, and set the GitHub `ec2`
environment variable `EC2_DEPLOY_SCRIPT` to `/root/deploy_ec2.sh` (the workflow
default uses the hyphenated filename). See [continuous deployment](../../docs/deployment.md).

Stack creation confirms EC2 creation, **not completion of software installation**:
there is no CloudFormation resource signal. Connect using the `SshCommand` stack
output, then verify:

```bash
sudo cloud-init status --wait --long
sudo test -f /var/lib/cloud/instance/matriz-provisioning-complete
git --version
sudo docker info
sudo docker compose version
node --version
npm --version
```

Installation failures are recorded in `/var/log/matriz-provisioning.log` and
`/var/log/cloud-init-output.log`. User data runs on first boot; updating it on an
existing instance does not automatically rerun installation.

This is host preparation only: application deployment, secrets, HTTPS/certificates,
DNS, backups and production acceptance remain separate. No Elastic IP is created;
the public address can change after stop/start or instance replacement. Creating
the stack provisions billable AWS resources. The template has not been deployed
or boot-tested against AWS.

Package reference: https://packages.ubuntu.com/resolute/amd64/docker-compose-v2
