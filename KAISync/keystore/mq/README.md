# Run RabbitMQ with SSL 

* Install Erlang from [here](https://www.erlang-solutions.com/downloads/download-erlang-otp)
* Install rabbitmq-server [here](http://www.rabbitmq.com/install-debian.html) 
* Copy cacert.cert, server/ to configuration file path (e.g /etc/rabbitmq)
* Move rabbitmq.config.sample to configuration file path (e.g /etc/rabbitmq/rabbitmq.config)
* Change cert file path in rabbitmq.config

PS. If you got error of eaccess in rabbitmq logs, then you need to change the owner of cacert path to rabbitmq:rabbitmq
