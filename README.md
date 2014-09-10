# YAHM - Yet Another HAProxy Manager

This project intends to be a manager process that uses:

* Zookeeper to keep state
* Marathon REST API to get list of tasks
* Gracefully reload HAProxy configuration
* Give the user full control over the configuration.

This project intends to be so light weight that the only thing that it provides is that it will just provide the very basic token replacement using moustache templates. These templates are defined by the user so anyone who is familiar with HAProxy templates can write very complex templates and it wil just work!

To get an idea of how this will work have a look at [HAProxyTemplateRendererTest](https://github.com/ankurcha/haproxy-marathon-manager/blob/master/src/test/java/com/malloc64/yahm/HaProxyTemplateRendererTest.java).

## Getting started

* Install HAProxy - `add-apt-repository -y ppa:vbernat/haproxy-1.5 && apt-get update && apt-get -y install haproxy`
* Install agent - Install the jar (I prefer `/opt/haproxy_manager` with configuration in `/etc/haproxy_manager.yaml`)

You may want to consider using monit or runit or upstart or something like that to manage the process.

* Start the agent - `java -jar haproxy_manager.jar server /etc/haproxy_manager.yaml`

The agent exposes the functionality as a REST API with the following JSON end points

### `POST /apps`

This end point takes are a `LoadbalancedApplication` as the data. The fields are:
* `id` : This corresponds to the marathon app id.
* `template`: HaProxy configuration template. (see configuration template variable section for details).
* `sslCertificate` (optional) : SSL certificate to use if SSL termination is needed.

```json
{
    "id": "/jenkins",
    "template": "backend collector-backend\n    mode http\n    balance roundrobin\n    option httpchk GET /private/status\n    option httplog\n    {{#tasks}}\n    server {{id}} {{host}}:{{port}} check\n    {{/tasks}}\n  frontend collector-http\n    bind metrics.brightcove.com:80\n    reqadd X-Forwarded-Proto:\\ http\n    default_backend collector-backend\n  frontend collector-https\n    bind metrics.brightcove.com:443 ssl crt {{certPath}}\n    reqadd X-Forwarded-Proto:\\ https\n    default_backend collector-backend"
}
```

### `GET /apps/{id}`
Gets the definition of the application configured as a `LoadbalancedApplication` object.

### `PUT /apps/{id}`
Same as `POST /apps` but updates the application configuration identified by `{id}`. This is useful in updating the template.

### `DELETE /apps/{id}`
Removes an existing application configuration for `{id}`. The changes are applied asynchronously.

## Configuration Template 

The proxy configuration for an app is basically a snippet of the haproxy.cfg file. 
Example configuration:

```
  backend example-backend
    mode http
    balance roundrobin
    option httpchk GET /private/status
    option httplog
    {{#tasks}}
    server {{id}} {{host}}:{{port}} check
    {{/tasks}}
  frontend example-http
    bind metrics.example.com:80
    reqadd X-Forwarded-Proto:\ http
    default_backend example-backend
  frontend example-https
    bind metrics.example.com:443 ssl crt {{certPath}}
    reqadd X-Forwarded-Proto:\ https
    default_backend example-backend
```

It can be anything and is passed through a [moustache compiler](https://github.com/spullara/mustache.java) which has the following variables at its disposal for token replacement.
* `tasks` - These are the individual tasks running as per mesos for the given application, these can be iterated (as shown in the example). They have the following fields:
  * `id` - unique identifier for a running task.
  * `host` - host on which the task is running.
  * `port` - port on which the task is listening.
* `certPath` - If an `sslCertificate` in the application configuration, this field will contain the path to that that certificate on the host.


## Known limitations
* Certificates are stored unencrypted in zookeeper.
* There is no authentication mechanisms for the rest endpoints.

## Roadmap
* Better documentation
* Stop polling API and use the marathon event subscriptions to get notification of change in configuration.
