# Total-Order-for-Multicast-Service
-----------------------------------------------------------------------------------------------------------------------------
INTRODUCTION :
-----------------------------------------------------------------------------------------------------------------------------
To implement Total order multicast service we must first understand what is meant by total ordering and multicast terms in general.

Total ordering of messages for a pair of processes Pi and Pj and for any given pair of messages m and n that is send to both of these processes implies that if message m is received before n at process Pi then message m is also received before n at Process Pj, i.e the message delivery order is same for all processes.

Multicasting of a message refers to sending the message to a group of receivers. Unlike unicasting of messages which is one to one communication technique and broadcasting which is one to all communication, multicasting is one to many communication technique. In this manner, User can send message to a selected group of receivers.

Hence to achieve total ordering for multicast messages we need to assure that for any set of processes receiving the multicast message, the order of the delivered message is same. This technique helps in efficiently mirroring of the data. In real scenario this technique is implemented in Replicated State machines (Mirroring of data) where data is replicated and stored at a different location in order to provide fault tolerance. 

-----------------------------------------------------------------------------------------------------------------------------
SYSTEM OVERVIEW :
-----------------------------------------------------------------------------------------------------------------------------
This project demonstrates how total ordering can be achieved for multicast messages in a distributed system using Skeen's algorithm. To produce a similar environment we will be running this project on different systems. Each System provides this feature using a single process and single or multiple threads per module.

Following are the features of each of these individual processes :-

- Serves both sender and receiver functionalities. 
- Has an ordering feature which assures that the ordering of the messages is same at all its destination processes. 
- A timestamp will be generated ( as per the Lamport’s logical clock rules) on every receive or send event and will be attached to the respective messages as per requirement. 
- Message format will be same throughout the system. 
- A Message buffer will be maintained and arranged in ascending order of the message timestamps. 
- Bookkeeping will be done inorder to ensure that a final timestamp is send only when all the processes in the multicast group have sent their timestamps.

An Overview of the entire system :-

- Process 1 multicasts msg to process 2 and 3 
- On receiving the msg both the processes 2 and 3,mark it undeliverable and send their timestamp values 2 and 4 respectively 
- Process 1 waits till all the receiver processes i.e process2 and 3 send their timestamp to it. 
- Process 1 then takes the max of the proposed timestamps and send out final timestamp to processes 2 and 3 
- Processes 2 and 3 mark msg as deliverable and deliver it if it has the smallest timestamp

-----------------------------------------------------------------------------------------------------------------------------
SYSTEM DESIGN :
-----------------------------------------------------------------------------------------------------------------------------
Interaction between sender and receiver thread :
The interactions between these threads will be when a timestamp is to be returned to intiator process on reception of a multicast message or when a final timestamp is to be send on reception of timestamps from all the receiver’s of the multicast message. In both of these cases sender thread will be informed by the receiver thread to send the respective messages.

Timestamping :
Lamport’s logical clock has been implemented to enable the timestamping feature. Since a timestamp is required for every send and receive events, So both the sender and receive threads will try accessing the timestamp value. To avoid corruption of the timestamp value, the system will use a synchronization technique and ensure that when sender thread is incrementing the timestamp value, receiver thread is waiting and vice-versa.

Total Ordering :
As per the algorithm all the receiver processes deliver a message only when the final timestamp of the message is obtained. Using this final timestamp, the messages are placed in the message buffer of a process. This ensures that the order of the messages is same for every process receiving these messages. Hence the ordering part is taken care of by the algorithm. From coding perspective the system maintains a buffer in which the messages can be easily inserted in between, beginning or end of the buffer and the lowest timestamp message is removed first.

-----------------------------------------------------------------------------------------------------------------------------
SYSTEM TESTING :
-----------------------------------------------------------------------------------------------------------------------------
These test cases check the working of the distributed system on the whole.

Few such test cases can be :-

- Message received at the other end is same as what was sent. Eg : if sent msg was “ Add $100 to Account A” then the received message should not be as “Add $100” i.e Message should not be trauncated.
- If a multicast message is send to processes p1,p2,p3 then it should be received by these 3 only, it shouldn’t be send to some 4th process or send to p1, p2 and p5(can happen if there is coding error, if msg is printed on some other socket also or wrong socket is connected to system’s ip address)
- Timestamp generated should follow Lamport’s rule, receive of a event should be less than send of the same event.
- Order of message delivery should be same at every process.

The System uses the log files of all the participating machines to analyze and find out if the test cases passed or failed. It then evaluates the final test result and displays the same.
