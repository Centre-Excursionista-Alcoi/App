#!/bin/bash

curl -X POST -H "Content-Type: application/json" -d "@sample_data/register.json" -u user1@example.com:Password123 -i http://127.0.0.1:8080/register
