<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Hive JDBC Driver Wrapper to Include Kerberos Authentication](#hive-jdbc-driver-wrapper-to-include-kerberos-authentication)
	- [Overview](#overview)
	- [How to build and Run](#how-to-build-and-run)
	- [Things you will want to customize](#things-you-will-want-to-customize)
	- [A note for deploying on windows](#a-note-for-deploying-on-windows)
	- [How to use this driver with your favourite SQL client](#how-to-use-this-driver-with-your-favourite-sql-client)
	- [BONUS: Dealing with SSL](#bonus-dealing-with-ssl)

<!-- /TOC -->

---

# Hive JDBC Driver Wrapper to Include Kerberos Authentication

## Overview

A JDBC driver wrapper for the Hadoop Hive driver, but with Kerberos taken care of. This will allow SQL clients wishing to connect to Hive, to just import a driver as a plain old JAR file, without worrying about all of the difficult Kerberos details (_provided you already have a valid TGT in your list_).

Particularly useful for SQL clients that do not support Kerberos authentication, but you would still like to use them to query a Kerberized Hadoop cluster, and you don't want to use Hue, beeline, etc. to hit Hive.

Kerberos is often painful and fiddly, so hopefully this can give someone else a head-start if they face a similar problem...

## How to build and Run

**Building...**

It's maven...sorry Gradle fans (of which I am one now):

```bash
$ mvn package -DskipTests
```

**Quick-Test...**

This will just kick off a quick `show databases` query so that you can quickly check to see if you're on the right track before [using the JAR in your favourite SQL client](#how-to-use-this-driver-with-your-favourite-sql-client).

```bash
$ java -jar hivedriverwrapper-1.0-SNAPSHOT.jar
or
$ java -jar hivedriverwrapper-1.0-SNAPSHOT-jar-with-dependencies.jar
```

*Obviously the version number might have changed*

**Unit Tests...**

**WARN:** No unit tests implemented for this project...

## Things you will want to customize

It should build right out of the box, which gives you a working driver that you just need to customise for your organisation:

- The `krb5.conf` and JAAS login configuration (see `src/main/resources`) will need to be tailored to your environment
- In `dps.publicexample.drivers`
    - You will need to alter `SEC_PROP_KRREALM` and `SEC_PROP_KRKDC` for your own environment

## A note for deploying on windows

If you see the error: `Configuration Error: No such file or directory`, followed by a stack trace showing exceptions being thrown from `sun.security.provider.*` then is likely because the application is not finding the JAAS login configuration. This is packaged inside the JAR, which then programmatically sets the system property `java.security.auth.login.config` so an issue with finding this can only point to a conflicting configuration made at the environmental level.

The first thing to check on Windows, would be the file: `%JAVA_HOME%/lib/security/java.security`. If the property `login.config.url.n` (where `n` is a number) has been set, then **this will cause issues**.

## How to use this driver with your favourite SQL client

**Step 1: Create a kerberos ticket**

```bash
// On Linux
$ kinit <user>@YOUR.DOMAIN.COM
and that is all that is required...

// On Windows
> cd $JAVA_HOME\lib\bin
> kinit -f <user>@YOUR.DOMAIN.COM
this will create a Java Krb5 cache file that will now be accessible/usable by the driver
```

**Step 2: Configure and use your SQL Client**

This will vary from vendor to vendor, but you will now be able to use the Uber-Jar like any other driver. You will just have to use the following information:

| Item                   |                 Value                    |
|-----------------------:|:-----------------------------------------|
|                  Class | `HiveWrapper`                            |
|            SQL Dialect | H2                                       |
|            Auto Commit | **Not** supported                        |
|              Auto Sync | **Not** supported                        |
| JDBC Connection String | `jdbc:hivecustom:`                       |

## BONUS: Dealing with SSL

If your connection is over SSL, you will need to take the following from the server:

1. impala demon private key
2. impala demon certificate
3. CA certificate

and convert them into a `jks`. This is a two step process:

**Step one: Convert x509 Cert and Key to a pkcs12 file**

```bash
$ openssl pkcs12 -export \
                 -in public-keystore.pem \
                -inkey host-keystore.key \
                -CAfile ca.pem \
                -caname impala \
                -out impalahost.p12 \
                -name impalahost \
                -chain \
                -password pass:asd123
```

**Step two: Convert the pkcs12 file to a java keystore**

```bash
$ keytool -importkeystore \
          -deststorepass asd123 \
          -destkeypass asd123 \
          -destkeystore impalahost.keystore \
          -srckeystore impalahost.p12 \
          -srcstoretype PKCS12 \
          -srcstorepass asd123 \
          -alias impalahost
```
