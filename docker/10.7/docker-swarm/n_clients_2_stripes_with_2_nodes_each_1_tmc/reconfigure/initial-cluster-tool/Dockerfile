FROM terracotta-cluster-tool:10.7.0-SNAPSHOT
COPY docker/orchestration/n_clients_2_stripes_with_2_nodes_each_1_tmc/reconfigure/initial-cluster-tool/stripe1.xml $SAG_HOME/stripe1.xml
COPY docker/orchestration/n_clients_2_stripes_with_2_nodes_each_1_tmc/reconfigure/initial-cluster-tool/stripe2.xml $SAG_HOME/stripe2.xml
COPY docker/orchestration/n_clients_2_stripes_with_2_nodes_each_1_tmc/reconfigure/initial-cluster-tool/Terracotta101.xml $LICENSES_DIRECTORY/license.xml
RUN sed -i "s|#!/bin/bash|#!/bin/bash\ncp /opt/softwareag/stripe1.xml $CONFIGS_DIRECTORY \&\& \
  cp /opt/softwareag/stripe2.xml $CONFIGS_DIRECTORY\n|" entrypoint.sh