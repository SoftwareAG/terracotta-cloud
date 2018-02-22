
Terracotta DB on AWS EC2
========================

What this document will help you accomplish:
--------------------------------------------

- Create a group of 5 AWS EC2 instances

- Install and start on 4 of them a Terracotta Server, part of a 2 stripes / 2 nodes each cluster

- Install and start the Terracotta Management Console (TMC) on the 5th AWS EC2 instance

- Run Ehcache3 clustered and / or TcStore clustered samples

Prerequisites:
--------------

- Install `aws cli` to connect with AWS
  https://aws.amazon.com/cli/
  
- Install `jq` to parse the aws cli json.
  https://stedolan.github.io/jq/
  
- Install netcat `nc` utility to check the connectivity with the AWS EC2 instances. http://netcat.sourceforge.net/
  
- Download the `Terracotta DB 10.2 kit` from http://www.terracotta.org/downloads/ and store it in the current directory.

- Have a Terracotta DB license file ready (you can get a trial license file from : http://www.terracotta.org/downloads/ )
  
- Run the following commands in `bash shell` only.


Steps:
------

- Set your downloaded Terracotta DB kit name in a bash shell environment

        export terracotta_kit=terracotta-db-10.2.0.0.XXX.tar.gz     # Update with your version.


- Set your download license file name in bash shell environment

        export license_file=license.xml     # Update with your file name.


- Set your aws account credentials (AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY) in bash shell environment.
  Example:
        
        export AWS_ACCESS_KEY_ID=AKIAIXGN3L7VLAUNKXEA
        export AWS_SECRET_ACCESS_KEY=C2Tot+TRf6LU+SLEfQJabvqC8CzFaaa/0sHiGp
        export AWS_DEFAULT_REGION=ca-central-1


- Create AWS EC2 `key pair`.
  
        key_pair_name="terracotta"      # Update different name if required.
        aws ec2 create-key-pair --key-name "$key_pair_name" | jq --raw-output '.KeyMaterial' > "$key_pair_name.pem"
        chmod 600 "$key_pair_name.pem"
        

- Create AWS EC2 security group so that we can connect to the created instances
 
        security_group_id=$(aws ec2 create-security-group --group-name "$key_pair_name" --description "$key_pair_name" | jq --raw-output '.GroupId')
        aws ec2 authorize-security-group-ingress --group-id "$security_group_id" --protocol tcp --port 22 --cidr 0.0.0.0/0  # Expose SSH port to outside world
        aws ec2 authorize-security-group-ingress --group-id "$security_group_id" --protocol tcp --port 9410 --source-group "$security_group_id"  # Expose terracotta port within the security group
        aws ec2 authorize-security-group-ingress --group-id "$security_group_id" --protocol tcp --port 9410 --cidr 0.0.0.0/0  # Expose terracotta port to outside world
        aws ec2 authorize-security-group-ingress --group-id "$security_group_id" --protocol tcp --port 9430 --source-group "$security_group_id"  # Expose terracotta sync port within the security group
        aws ec2 authorize-security-group-ingress --group-id "$security_group_id" --protocol tcp --port 9430 --cidr 0.0.0.0/0  # Expose sync port to outside world
        aws ec2 authorize-security-group-ingress --group-id "$security_group_id" --protocol tcp --port 9480 --cidr 0.0.0.0/0  # Expose TMC port to outside world
        
        
- Identify the AWS EC2 `ami id` to install Terracotta on.     

        ami_id=$(aws ec2 describe-images --owners amazon --filters 'Name=name,Values=amzn-ami-hvm-????.??.?.????????-x86_64-gp2' 'Name=state,Values=available' | jq -r '.Images | sort_by(.CreationDate) | last(.[]).ImageId')
        
        
- Create AWS EC2 instance using the identified ami id.

        instance_ids=($(aws ec2 run-instances --image-id $ami_id --count 5 --instance-type m4.2xlarge --key-name $key_pair_name --security-group-ids $security_group_id | jq --raw-output '.Instances[].InstanceId' | xargs echo))
    
    
- Identify the public DNS names of the running AWS EC2 instances.

        public_dns_names=($(aws ec2 describe-instances --instance-ids ${instance_ids[@]} | jq --raw-output '.Reservations[].Instances[].PublicDnsName' | xargs echo))
        

- Wait for instances to be started and available to connect.

        for public_dns in ${public_dns_names[@]}; do
            while [ true ]; do
                nc -z "$public_dns" 22
                if [ $? -eq 0 ]; then
                    break
                else
                    sleep 3
                fi
            done
        done


- Install Java 8 on all the started instances.
 
        for public_dns in ${public_dns_names[@]}; do
            ssh -i "$key_pair_name.pem" -oStrictHostKeyChecking=no "ec2-user@$public_dns" 'sudo yum -y install java-1.8.0 && sudo yum -y remove java-1.7.0-openjdk && java -version'
        done
        
- Upload Terracotta kit on all the started AWS EC2 instances.

        for public_dns in ${public_dns_names[@]}; do
        
            # Upload the Terracotta kit to the instance
            scp -i "$key_pair_name.pem" -oStrictHostKeyChecking=no $terracotta_kit "ec2-user@$public_dns:"
            
            # Extract the uploaded Terracotta kit.
            ssh -i "$key_pair_name.pem" -oStrictHostKeyChecking=no "ec2-user@$public_dns" "tar xvzf $terracotta_kit"
        done
        
        
- Creating Terracotta configuration Template
        
        tc_config='<?xml version="1.0" encoding="UTF-8"?>
                   <tc-config xmlns="http://www.terracotta.org/config">
                   
                     <plugins>
                       <config>
                         <ohr:offheap-resources xmlns:data="http://www.terracottatech.com/config/data-roots" xmlns:ohr="http://www.terracotta.org/config/offheap-resource">
                           <ohr:resource name="primary-server-resource" unit="MB">512</ohr:resource>
                         </ohr:offheap-resources>
                       </config>
                       <config>
                         <data:data-directories xmlns:data="http://www.terracottatech.com/config/data-roots" xmlns:ohr="http://www.terracotta.org/config/offheap-resource">
                           <data:directory name="data" use-for-platform="true">terracotta-data</data:directory>
                         </data:data-directories>
                       </config>
                     </plugins>
                     <servers>
                       <server host="HOST1">
                       </server>
                       <server host="HOST2">
                       </server>
                     </servers>
                   
                   </tc-config>'


- Creating Terracotta configuration
        
        # create tc_config file for both the stripes
        echo $(echo $tc_config | sed -e "s/HOST1/${public_dns_names[0]}/g" | sed -e "s/HOST2/${public_dns_names[1]}/g") > tc_config1.xml
        echo $(echo $tc_config | sed -e "s/HOST1/${public_dns_names[2]}/g" | sed -e "s/HOST2/${public_dns_names[3]}/g") > tc_config2.xml
 

- Push tc_configs to AWS EC2 instances
        
        # Start the Terracotta server on the created instance
        scp -i "$key_pair_name.pem" -oStrictHostKeyChecking=no tc_config1.xml "ec2-user@${public_dns_names[0]}:tc_config.xml"
        scp -i "$key_pair_name.pem" -oStrictHostKeyChecking=no tc_config1.xml "ec2-user@${public_dns_names[1]}:tc_config.xml"
        scp -i "$key_pair_name.pem" -oStrictHostKeyChecking=no tc_config2.xml "ec2-user@${public_dns_names[2]}:tc_config.xml"
        scp -i "$key_pair_name.pem" -oStrictHostKeyChecking=no tc_config2.xml "ec2-user@${public_dns_names[3]}:tc_config.xml"


- SSH into the started AWS EC2 instances and run the Terracotta server with appropriate config.
        
        # Identify the kit name
        kit_name=$(echo $terracotta_kit | sed -e "s/.tar.gz//g" | sed -e "s/.zip//g")

        # Start the Terracotta server on the created instance
        for public_dns in ${public_dns_names[@]::4}; do
            
            ssh -i "$key_pair_name.pem" -oStrictHostKeyChecking=no "ec2-user@$public_dns" "nohup $kit_name/server/bin/start-tc-server.sh -f tc_config.xml > terracotta.out 2> terracotta.err < /dev/null &"
            
        done
        

- Configure the cluster using cluster tool.
        
        # Upload the license file to the required place
        scp -i "$key_pair_name.pem" -oStrictHostKeyChecking=no $license_file "ec2-user@${public_dns_names[4]}:$kit_name/tools/cluster-tool/conf/license.xml"
        
        # Run the cluster tool configure command
        ssh -i "$key_pair_name.pem" -oStrictHostKeyChecking=no "ec2-user@${public_dns_names[4]}" "$kit_name/tools/cluster-tool/bin/cluster-tool.sh configure -n MyCluster -s ${public_dns_names[0]} -s ${public_dns_names[1]} -s ${public_dns_names[2]} -s ${public_dns_names[3]}"
        
        
- Start TMC on the last AWS EC2 instance

       tmc_default_url="terracotta://${public_dns_names[0]}:9410,${public_dns_names[1]}:9410,${public_dns_names[2]}:9410,${public_dns_names[3]}:9410"
       ssh -i "$key_pair_name.pem" -oStrictHostKeyChecking=no "ec2-user@${public_dns_names[4]}" "export TMS_DEFAULTURL=$tmc_default_url; nohup $kit_name/tools/management/bin/start.sh > management.out 2> management.err < /dev/null &"
               

- Start the ehcache3 and tcstore client samples

        # untar the local kit to be able to use it locally to run the samples
        tar xvzf terracotta-db-10.2.0.0.XXX.tar.gz

        # set the required environment variable for the samples
        export TC_HOME=terracotta-db-10.2.0.0.XXX/

        # Clone the Terracotta DB samples github repository
        git clone https://github.com/SoftwareAG/terracotta-db-samples
        
        # exporting environment variable so that samples can connect to our AWS cluster.
        export TERRACOTTA_SERVER_URL="terracotta://${public_dns_names[0]}:9410,${public_dns_names[1]}:9410,${public_dns_names[2]}:9410,${public_dns_names[3]}:9410"
        
        # Run any sample within the downloaded repo, example:
        cd terracotta-db-samples/ehcache-example01-clustered-cache/
        ./start-client.(sh|bat)
            
        The samples automatically connect to cluster running at TERRACOTTA_SERVER_URL environment variable.
        
- Connect to TMC to check the cluster status

        echo "TMC is reachable at 'http://${public_dns_names[4]}:9480'"


Cleaning things up :
--------------------

- Terminate all EC2 instances

        aws ec2 terminate-instances --instance-ids ${instance_ids[@]}| jq --raw-output '.TerminatingInstances[].CurrentState.Name'

- After few seconds, make sure they're all terminated

        aws ec2 terminate-instances --instance-ids ${instance_ids[@]}| jq --raw-output '.TerminatingInstances[].CurrentState.Name'

- Delete the key pair

        aws ec2 delete-key-pair --key-name "$key_pair_name"

- Delete the security group id

        aws ec2 delete-security-group --group-id "$security_group_id"
