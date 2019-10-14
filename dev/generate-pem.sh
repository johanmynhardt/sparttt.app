#! /usr/bin/env bash

if [[ -f "dev-key.pem" ]]; then
	echo "pem found. No need to generate!";
else
	echo "no pem! Generating...";
	openssl req -new -x509 -keyout dev-key.pem -out dev-key.pem -days 365 -nodes
fi
