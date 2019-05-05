# Web Services Queue Producer

**DRAFT** - The code here is purely for personal experimentation and not intended for public demonstration of any kind. 

Implementation of a CXF Web Service that eventually sends response to RabbitMQ

This is primarily an exploration of a moderatly complex threading scenario for the purpose of learning more about how to use Java threading.

The general scenario is;

Each web service requests are sent using individual async task 
There is a singleton Rabbit MQ consumer that receives all results 

Given many request to be sent to  web service   
And immediate reply from web service with job identifier   
And long wait for job processing result to be placed on message queue   
When we send 20 requests we process only 4 at same time waiting until have revived result before starting next one 
Then if any request fails at any stage we abort sending any further requests 
And if we take to long to get a result on the queue for a give request we abort sending any further requests 

## Github practice repository

This repository is also, for me, a practise one for using Github.  
For this reason there will be atypical activity that is purely for the purpose of exploring Github usage.
