/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.location.jclouds.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.BasicConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.location.LocationConfigKeys;
import org.apache.brooklyn.core.location.access.BrooklynAccessUtils;
import org.apache.brooklyn.core.location.access.PortForwardManager;
import org.apache.brooklyn.core.location.cloud.CloudLocationConfig;
import org.apache.brooklyn.util.core.internal.ssh.SshTool;
import org.jclouds.Constants;

import com.google.common.annotations.Beta;
import com.google.common.reflect.TypeToken;

public interface JcloudsLocationConfigPublic extends CloudLocationConfig {

    public static final ConfigKey<String> CLOUD_PROVIDER = LocationConfigKeys.CLOUD_PROVIDER;

    public static final ConfigKey<Boolean> RUN_AS_ROOT = ConfigKeys.newBooleanConfigKey("runAsRoot", 
            "Whether to run initial setup as root (default true)", null);
    public static final ConfigKey<String> LOGIN_USER = ConfigKeys.newStringConfigKey("loginUser",
            "Override the user who logs in initially to perform setup " +
            "(otherwise it is detected from the cloud or known defaults in cloud or VM OS)", null);
    public static final ConfigKey<String> LOGIN_USER_PASSWORD = ConfigKeys.newStringConfigKey("loginUser.password",
            "Custom password for the user who logs in initially", null);
    public static final ConfigKey<String> LOGIN_USER_PRIVATE_KEY_DATA = ConfigKeys.newStringConfigKey("loginUser.privateKeyData",
            "Custom private key for the user who logs in initially", null);   
    // not supported in jclouds
//    public static final ConfigKey<String> LOGIN_USER_PRIVATE_KEY_PASSPHRASE = ConfigKeys.newStringKey("loginUser.privateKeyPassphrase", 
//            "Passphrase for the custom private key for the user who logs in initially", null);
    public static final ConfigKey<String> LOGIN_USER_PRIVATE_KEY_FILE = ConfigKeys.newStringConfigKey("loginUser.privateKeyFile",
            "Custom private key for the user who logs in initially", null); 
    public static final ConfigKey<String> EXTRA_PUBLIC_KEY_DATA_TO_AUTH = ConfigKeys.newStringConfigKey("extraSshPublicKeyData",
        "Additional public key data to add to authorized_keys (multi-line string supported, with one key per line)", null);
    @SuppressWarnings("serial")
    public static final ConfigKey<List<String>> EXTRA_PUBLIC_KEY_URLS_TO_AUTH = ConfigKeys.newConfigKey(new TypeToken<List<String>>() {}, 
        "extraSshPublicKeyUrls", "Additional public keys (files or URLs, in SSH2/RFC4716/id_rsa.pub format) to add to authorized_keys", null);

    public static final ConfigKey<String> KEY_PAIR = ConfigKeys.newStringConfigKey("keyPair",
        "Custom keypair (name) known at the cloud to be installed on machines for initial login (selected clouds only); "
        + "you may also need to set "+LOGIN_USER_PRIVATE_KEY_FILE.getName(), null);
    public static final ConfigKey<Boolean> AUTO_GENERATE_KEYPAIRS = ConfigKeys.newBooleanConfigKey("jclouds.openstack-nova.auto-generate-keypairs",
        "Whether to generate keypairs automatically (OpenStack Nova)");

    /** @deprecated since 0.9.0 Use {@link #AUTO_ASSIGN_FLOATING_IP} instead (deprecated in 0.7.0 but warning not given until 0.9.0) */
    @Deprecated
    public static final ConfigKey<Boolean> AUTO_CREATE_FLOATING_IPS = ConfigKeys.newBooleanConfigKey("jclouds.openstack-nova.auto-create-floating-ips",
            "Whether to generate floating ips for Nova");
    public static final ConfigKey<Boolean> AUTO_ASSIGN_FLOATING_IP = ConfigKeys.newBooleanConfigKey("autoAssignFloatingIp",
            "Whether to generate floating ips (in Nova paralance), or elastic IPs (in CloudStack parlance)");

    public static final ConfigKey<Boolean> DONT_CREATE_USER = ConfigKeys.newBooleanConfigKey("dontCreateUser",
            "Whether to skip creation of 'user' when provisioning machines (default false). " +
            "Note that setting this will prevent jclouds from overwriting /etc/sudoers which might be " +
            "configured incorrectly by default. See 'dontRequireTtyForSudo' for details.",
            false);
    public static final ConfigKey<Boolean> GRANT_USER_SUDO = ConfigKeys.newBooleanConfigKey("grantUserSudo",
            "Whether to grant the created user sudo privileges. Irrelevant if dontCreateUser is true. Default: true.", true);
    public static final ConfigKey<Boolean> DISABLE_ROOT_AND_PASSWORD_SSH = ConfigKeys.newBooleanConfigKey("disableRootAndPasswordSsh",
        "Whether to disable direct SSH access for root and disable password-based SSH, "
        + "if creating a user with a key-based login; "
        + "defaults to true (set false to leave root users alone)", true);
    public static final ConfigKey<String> CUSTOM_TEMPLATE_OPTIONS_SCRIPT_CONTENTS = ConfigKeys.newStringConfigKey("customTemplateOptionsScriptContents",
        "A custom script to pass to jclouds as part of template options, run after AdminAccess, "
        + "for use primarily where a command which must run as root on first login before switching to the admin user, "
        + "e.g. to customize sudoers; may start in an odd location (e.g. /tmp/bootstrap); "
        + "NB: most commands should be run by entities, or if VM-specific but sudo is okay, then via setup.script, not via this");
    
    public static final ConfigKey<String> GROUP_ID = ConfigKeys.newStringConfigKey("groupId",
            "The Jclouds group provisioned machines should be members of. " +
            "Users of this config key are also responsible for configuring security groups.");
    
    // jclouds compatibility
    public static final ConfigKey<String> JCLOUDS_KEY_USERNAME = ConfigKeys.newStringConfigKey(
            "userName", "Equivalent to 'user'; provided for jclouds compatibility", null);
    public static final ConfigKey<String> JCLOUDS_KEY_ENDPOINT = ConfigKeys.newStringConfigKey(
            Constants.PROPERTY_ENDPOINT, "Equivalent to 'endpoint'; provided for jclouds compatibility", null);
    
    // note causing problems on centos due to use of `sudo -n`; but required for default RHEL VM
    /**
     * @deprecated since 0.8.0; instead configure this on the entity. See SoftwareProcess.OPEN_IPTABLES.
     */
    @Deprecated
    public static final ConfigKey<Boolean> OPEN_IPTABLES = ConfigKeys.newBooleanConfigKey("openIptables", 
            "[DEPRECATED - use openIptables on SoftwareProcess entity] Whether to open the INBOUND_PORTS via iptables rules; " +
            "if true then ssh in to run iptables commands, as part of machine provisioning", true);

    /**
     * @deprecated since 0.8.0; instead configure this on the entity. See SoftwareProcess.STOP_IPTABLES.
     */
    @Deprecated
    public static final ConfigKey<Boolean> STOP_IPTABLES = ConfigKeys.newBooleanConfigKey("stopIptables", 
            "[DEPRECATED - use stopIptables on SoftwareProcess entity] Whether to stop iptables entirely; " +
            "if true then ssh in to stop the iptables service, as part of machine provisioning", false);

    public static final ConfigKey<String> HARDWARE_ID = ConfigKeys.newStringConfigKey("hardwareId",
            "A system-specific identifier for the hardware profile or machine type to be used when creating a VM", null);
    
    public static final ConfigKey<String> IMAGE_ID = ConfigKeys.newStringConfigKey("imageId", 
            "A system-specific identifier for the VM image to be used when creating a VM", null);
    public static final ConfigKey<String> IMAGE_NAME_REGEX = ConfigKeys.newStringConfigKey("imageNameRegex", 
            "A regular expression to be compared against the 'name' when selecting the VM image to be used when creating a VM", null);
    public static final ConfigKey<String> IMAGE_DESCRIPTION_REGEX = ConfigKeys.newStringConfigKey("imageDescriptionRegex", 
            "A regular expression to be compared against the 'description' when selecting the VM image to be used when creating a VM", null);

    public static final ConfigKey<String> TEMPLATE_SPEC = ConfigKeys.newStringConfigKey("templateSpec", 
            "A jclouds 'spec' string consisting of properties and values to be used when creating a VM " +
            "(in most cases the properties can, and should, be specified individually using other Brooklyn location config keys)", null);

    public static final ConfigKey<String> DEFAULT_IMAGE_ID = ConfigKeys.newStringConfigKey("defaultImageId", 
            "A system-specific identifier for the VM image to be used by default when creating a VM " +
            "(if no other VM image selection criteria are supplied)", null);

    public static final ConfigKey<Object> SECURITY_GROUPS = new BasicConfigKey<Object>(Object.class, "securityGroups",
            "Security groups to be applied when creating a VM, on supported clouds " +
            "(either a single group identifier as a String, or an Iterable<String> or String[])", null);

    public static final ConfigKey<String> USER_METADATA_STRING = ConfigKeys.newStringConfigKey("userMetadataString", 
        "Arbitrary user data, as a single string, on supported clouds (AWS)", null);

    @Deprecated /** @deprecated since 0.7.0 even AWS (the only one where this was supported) does not seem to want this uuencoded;
      use #USER_METADATA_STRING */
    public static final ConfigKey<String> USER_DATA_UUENCODED = ConfigKeys.newStringConfigKey("userData", 
        "Arbitrary user data, as a single string in uuencoded format, on supported clouds (AWS)", null);

    public static final ConfigKey<Object> STRING_TAGS = new BasicConfigKey<Object>(Object.class, "tags", 
            "Tags to be applied when creating a VM, on supported clouds " +
            "(either a single tag as a String, or an Iterable<String> or String[];" +
            "note this is not key-value pairs (e.g. what AWS calls 'tags'), for that see userMetadata)", null);

    @Deprecated /** @deprecated since 0.7.0 use #STRING_TAGS */
    public static final ConfigKey<Object> TAGS = STRING_TAGS;

    public static final ConfigKey<Object> USER_METADATA_MAP = new BasicConfigKey<Object>(Object.class, "userMetadata", 
            "Arbitrary user metadata, as a map (or String of comma-separated key=value pairs), on supported clouds; " +
            "note often values cannot be null", null);
    @Deprecated /** @deprecated since 0.7.0 use #USER_METADATA_MAP */
    public static final ConfigKey<Object> USER_METADATA = USER_METADATA_MAP;

    public static final ConfigKey<Boolean> INCLUDE_BROOKLYN_USER_METADATA = ConfigKeys.newBooleanConfigKey("includeBrooklynUserMetadata", 
        "Whether to set metadata about the context of a machine, e.g. brooklyn-entity-id, brooklyn-app-name (default true)", true);

    // See also SoftwareProcess.DONT_REQUIRE_TTY_FOR_SUDO
    public static final ConfigKey<Boolean> DONT_REQUIRE_TTY_FOR_SUDO = ConfigKeys.newBooleanConfigKey("dontRequireTtyForSudo",
            "Whether to explicitly set /etc/sudoers, so don't need tty (will leave unchanged if 'false'); " +
            "some machines require a tty for sudo; brooklyn by default does not use a tty " +
            "(so that it can get separate error+stdout streams); you can enable a tty as an " +
            "option to every ssh command, or you can do it once and " +
            "modify the machine so that a tty is not subsequently required. " +
            "Usually used in conjunction with 'dontCreateUser' since it will prevent " +
            "jclouds from overwriting /etc/sudoers and overriding the system default. " +
            "When not explicitly set will be applied if 'dontCreateUser' is set.");

    public static final ConfigKey<Boolean> MAP_DEV_RANDOM_TO_DEV_URANDOM = ConfigKeys.newBooleanConfigKey(
            "installDevUrandom", "Map /dev/random to /dev/urandom to prevent halting on insufficient entropy", true);

    public static final ConfigKey<String> LOCAL_TEMP_DIR = SshTool.PROP_LOCAL_TEMP_DIR;
    
    public static final ConfigKey<Integer> OVERRIDE_RAM = ConfigKeys.newIntegerConfigKey("overrideRam", "Custom ram value");    
    
    public static final ConfigKey<String> NETWORK_NAME = ConfigKeys.newStringConfigKey(
        "networkName", "Network name or ID where the instance should be created (e.g. the subnet ID in AWS");

    /**
     * CUSTOM_MACHINE_SETUP_SCRIPT_URL accepts a URL location that points to a shell script. 
     * Please have a look at locations/jclouds/src/main/resources/org/apache/brooklyn/location/jclouds/sample/setup-server.sh as an example
     */
    public static final ConfigKey<String> CUSTOM_MACHINE_SETUP_SCRIPT_URL = ConfigKeys.newStringConfigKey(
            "setup.script", "Custom script to customize a node");
    
    @SuppressWarnings("serial")
    public static final ConfigKey<List<String>> CUSTOM_MACHINE_SETUP_SCRIPT_URL_LIST = ConfigKeys.newConfigKey(new TypeToken<List<String>>() {},
            "setup.scripts", "A list of scripts to customize a node");
    
    public static final ConfigKey<String> CUSTOM_MACHINE_SETUP_SCRIPT_VARS = ConfigKeys.newStringConfigKey(
            "setup.script.vars", "vars to customize a setup.script i.e.: key1:value1,key2:value2");
    
    public static final ConfigKey<Boolean> GENERATE_HOSTNAME = ConfigKeys.newBooleanConfigKey(
            "generate.hostname", "Use the nodename generated by jclouds", false);

    public static final ConfigKey<Boolean> USE_PORT_FORWARDING = ConfigKeys.newBooleanConfigKey(
            "portforwarding.enabled", 
            "Whether to setup port-forwarding to subsequently access the VM (over the ssh port)",
            false);
    
    @Beta
    public static final ConfigKey<Boolean> USE_JCLOUDS_SSH_INIT = ConfigKeys.newBooleanConfigKey(
            "useJcloudsSshInit", 
            "Whether to use jclouds for initial ssh-based setup (i.e. as part of the 'TemplateOptions'); "
                    + "if false will use core brooklyn ssh utilities. "
                    + "This config is beta; its default could be changed and/or the option removed in an upcoming release.", 
            true);
    
    @Beta
    public static final ConfigKey<Boolean> LOOKUP_AWS_HOSTNAME = ConfigKeys.newBooleanConfigKey(
            "lookupAwsHostname", 
            "Whether to lookup the AWS hostname (via a command on the VM), or to just use the IP.", 
            true);
    
    public static final ConfigKey<PortForwardManager> PORT_FORWARDING_MANAGER = BrooklynAccessUtils.PORT_FORWARDING_MANAGER;

    public static final ConfigKey<Integer> MACHINE_CREATE_ATTEMPTS = ConfigKeys.newIntegerConfigKey(
            "machineCreateAttempts", "Number of times to retry if jclouds fails to create a VM", 2);

    public static final ConfigKey<Integer> MAX_CONCURRENT_MACHINE_CREATIONS = ConfigKeys.newIntegerConfigKey(
            "maxConcurrentMachineCreations", "Maximum number of concurrent machine-creations", Integer.MAX_VALUE);

    public static final ConfigKey<Semaphore> MACHINE_CREATION_SEMAPHORE = ConfigKeys.newConfigKey(
            Semaphore.class, "machineCreationSemaphore", "Semaphore for controlling concurrent machine creation", null);
    
    @SuppressWarnings("serial")
    public static final ConfigKey<Map<String,Object>> TEMPLATE_OPTIONS = ConfigKeys.newConfigKey(
            new TypeToken<Map<String, Object>>() {}, "templateOptions", "Additional jclouds template options");

    /**
     * The purpose of this config is to aid deployment of blueprints to clouds where it is difficult to get nodes to
     * communicate via private IPs. This config allows a user to overwrite private IPs as public IPs, thus ensuring
     * that any blueprints they wish to deploy which may use private IPs still work in these clouds.
     */
    @Beta
    public static final ConfigKey<Boolean> USE_MACHINE_PUBLIC_ADDRESS_AS_PRIVATE_ADDRESS = ConfigKeys.newBooleanConfigKey(
            "useMachinePublicAddressAsPrivateAddress",
            "When true we will use the public IP/Hostname of a JClouds Location as the private IP/Hostname",
            false);

}

