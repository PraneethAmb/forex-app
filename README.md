# Forex-app

This is a simple application that acts as a local proxy for getting exchange rates using [One-Frame API](https://hub.docker.com/r/paidyinc/one-frame)

The  proxy server support the following two requirments.

1. User of the application should be able to ask for an exchange rate between 2 given currencies, and get back a rate that is not older than 5 minutes
2. The application should at least support 10.000 requests per day.

## Prerequisites

* SBT
* Docker
* Curl

## Executing the Application
1. Clone the github repo
2. Navigate to /forex-mtl
3. Run the following command
```
sudo docker-compose up
```
4. After the redis and One frame docker containers are running, navigate to a another terminal
5. Run the following command from /forex-mtl
```
sudo sbt run
```
6. After the server has started, you can query echange rates using curl 
```
curl --location --request GET 'http://  localhost:8080/v1/rates?from=USD&to=JPY' \
--header 'X-Auth-Regular-User: 10dc303535874aeccc86a8251e6992f5'
``` 
### Supported Currencies
following are the list of currencies supported

+ AUD
+ CAD
+ CHF
+ EUR
+ GBP
+ NZD
+ JPY
+ SGD
+ USD

## Implementation
The proxy server is implimented as follows.
when a request comres from the client,
* The server checks the local storage last updated time and its status

* if last update time is less than 5 minutes and its status is valid. sever retrieves the exchange rate from the local storage and return to client.

+ if last update time is greater than 5 minutes and status is valid, server invalidates the local storage, fetch all currency exchange rates via one request to the one frame api service, and updates the local storage. After which returns the client the requested exchange rate.

* if status in invalid, (which means another node, thread etc... is already updateing the local storage), it waits till the local storage is updated, after which sends the client with the requested exchange rate.


## Simplifications and Assumptions
* The application uses. http and not https
* The application uses simple token based authentication. And currently only support the following token (should be set in X-Auth-Regular-User header)
```
10dc303535874aeccc86a8251e6992f5
```
+ Tokens are stored directly in config file.
* It is assumed the time difference between processing one pair of currency and processing multiple pair of currencies in One-Frame service is negligable.

## TODO
* Unit, Integration, Performance Testing
