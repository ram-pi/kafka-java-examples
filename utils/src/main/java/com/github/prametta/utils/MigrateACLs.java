package com.github.prametta.utils;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.utils.Utils;

@Log4j2
public class MigrateACLs {

  @SneakyThrows
  public static void main(String[] args) {
    // if args length is not 2, then throw an exception
    if (args.length != 2) {
      log.error(
          "Usage: MigrateACLs <clientPropertiesPathFileSource> <clientPropertiesPathFileDest>");
      throw new IllegalArgumentException(
          "Usage: MigrateACLs <clientPropertiesPathFileSource> <clientPropertiesPathFileDest>");
    }

    String clientPropertiesPathFileSource = args[0];
    String clientPropertiesPathFileDest = args[1];

    // if clientPropertiesPathFileSource is not a file, then throw an exception
    if (!new File(clientPropertiesPathFileSource).isFile()) {
      log.error("The file {} does not exist.", clientPropertiesPathFileSource);
      throw new IllegalArgumentException(
          "The file " + clientPropertiesPathFileSource + " does not exist.");
    }

    // if clientPropertiesPathFileDest is not a file, then throw an exception
    if (!new File(clientPropertiesPathFileDest).isFile()) {
      log.error("The file {} does not exist.", clientPropertiesPathFileDest);
      throw new IllegalArgumentException(
          "The file " + clientPropertiesPathFileDest + " does not exist.");
    }

    // Create kafka admin client
    AdminClient kafkaAdminClientSource =
        KafkaAdminClient.create(Utils.loadProps(clientPropertiesPathFileSource));
    AdminClient kafkaAdminClientDest =
        KafkaAdminClient.create(Utils.loadProps(clientPropertiesPathFileDest));

    Collection<AclBinding> sourceACLs =
        kafkaAdminClientSource.describeAcls(AclBindingFilter.ANY).values().get();

    log.info("Found {} ACLs to migrate.", sourceACLs.size());

    sourceACLs.forEach(
        aclBinding -> {
          log.info("Creating ACL: {}", aclBinding);
          KafkaFuture<Void> futures =
              kafkaAdminClientDest.createAcls(Collections.singleton(aclBinding)).all();

          // wait for the future to complete
          try {
            futures.get();
          } catch (Exception e) {
            log.error("Failed to create ACL: {}", aclBinding, e);
          }
        });

    // print destination cluster ACL total
    log.info(
        "Destination cluster ACL total: {}",
        kafkaAdminClientDest.describeAcls(AclBindingFilter.ANY).values().get().size());

    // close the kafka admin client
    kafkaAdminClientSource.close();
    kafkaAdminClientDest.close();

    log.info("ACLs migration completed.");
  }
}
