#!/bin/bash

count=0
until [ $count -gt 20 ]; do
  time ./a.out
  ((count++))
done