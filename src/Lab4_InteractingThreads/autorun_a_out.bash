#!/bin/bash

count=0
until [ $count -gt 10 ]; do
  time ./a.out
  ((count++))
done