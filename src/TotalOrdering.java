import java.util.*;
import java.io.*;
import java.net.*;
import java.util.logging.*;


class Sender extends Thread {
	String sname;
	Timestamp sendts;
	MiscellaneousWork mw;
	Logger mylogger;

	
	public Sender(Timestamp tstamp, String sname, MiscellaneousWork mw, Logger mylogger) {
		sendts = tstamp;
		this.sname = sname;
		this.mw = mw;
		this.mylogger = mylogger;
	}
	
	public synchronized void m_send(String[] host, String msgbody, int n) throws InterruptedException { //sends a new msg
		int j, port =25000;
		String hostname;
		Socket socket;
		InetAddress address;
		port = Integer.parseInt(mw.read_config_file("port"));
		for(j=0;j<n; j++) {
			try {
				address = (InetAddress)null;
				hostname = mw.read_config_file(host[j]);
				address = InetAddress.getByName(hostname);
				socket = new Socket(address, port);

				OutputStream os = socket.getOutputStream();
				OutputStreamWriter osw = new OutputStreamWriter(os);
				BufferedWriter bw = new BufferedWriter(osw);
				
				bw.write(msgbody);
				bw.flush();
				mylogger.info("Sender Thread : Message sent to host : "+host[j]);
				mylogger.info("-------------------------------------------------------------");
				System.out.println("Sender Thread : Message sent to host : "+host[j]);
				System.out.println("-------------------------------------------------------------");
				socket.close();
			}catch(IOException e) {
				System.out.println("Sender Thread : Host "+host[j]+" not found\n");
				mylogger.info("Sender Thread : Host "+host[j]+" not found\n");
				mylogger.info("----x--------x----------x------------x-----------x--------x--------");
			}
		}
		mylogger.info("\nSender Thread : Sent Message is :");
		mylogger.info(msgbody);
		mylogger.info("-------------------------------------------------------------");
		System.out.println("\nSender Thread : Sent Message is :");
		System.out.println(msgbody);
		System.out.println("-------------------------------------------------------------");
	
	}


	public void run() {
		int k =0,msgcount =1,n=0,msgno=0;
		String str,msg,msgbody,pno=null;
		BookkeepingMsgs bmsgs = new BookkeepingMsgs(mylogger);
		MsgDeliveryInfo msg_dinfo;
		Scanner in = new Scanner(System.in);
		//can add sleep here 
		while (true) {
			//mylogger.info("enter msg to be sent :");
			msg = in.nextLine();
			try {
				if (msg != null) {
				//msg body creation
					if(sname.substring(3,4).equals("0"))
						pno = sname.substring(4);
					else
						pno = sname.substring(3);
					mylogger.info("My machine name is : "+sname);
					mylogger.info("My Process no is : "+pno);
					msgno = Integer.parseInt(pno+"0"+msgcount);	
					msgbody = "Msg Type: New Msg ||\n"+"Msg No: "+pno+"0"+msgcount+" ||\nSender: "+
					sname+ " ||\nMsg: "+ msg + " ||\nMsg Timestamp: "+ sendts.getts(0);
					msgbody = msgbody + "\n";
					System.out.println("\nEnter no of receivers to whom this message should be sent:");
					str = in.nextLine();
					n = Integer.parseInt(str);
					String host[]= new String[n];
					for(k = 0;k<n;k++) {
						System.out.println("\nEnter the machine name of the receiver :");
						host[k] = in.nextLine();
					}
					msgcount++;
					msg_dinfo = new MsgDeliveryInfo(msgno,host,n,mylogger);
					mylogger.info("Sender Thread : adding sent message info to bookkeeping table");
					bmsgs.add_new_msg_info(msg_dinfo);
					m_send(host,msgbody,n);
				}
			}catch(InterruptedException e) {
				System.out.println("Sender Thread : Error in synchronizing threads\n");
				mylogger.info("----x--------x----------x------------x-----------x--------x--------");
			}
		}

	}
}

class Receiver extends Thread {
	String rcvdmsg;
	Timestamp rcvts;
	SyncMsgs syncmsg;
	MiscellaneousWork mw;
	Logger mylogger;

	public Receiver(Timestamp tstamp, SyncMsgs syncmsg,MiscellaneousWork mw, Logger mylogger) {
		rcvts = tstamp;
		this.syncmsg = syncmsg;
		this.mw = mw;
		this.mylogger = mylogger;
	}
	public int read_senderts(String msgbody) {
		int index=0,senderts=0;
		String str;
		index = msgbody.lastIndexOf(':');
		str = msgbody.substring(index+2);	
		senderts = Integer.parseInt(str);
		return senderts;
	}

	public void m_rcv() {
		int port = 0,ts=0;
		String msgbody = null, readmsg=null;
		RcvdMsgInfo msginfo = new RcvdMsgInfo();
		Node rcvdmsgnode = new Node();
		port = Integer.parseInt(mw.read_config_file("port"));
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			//mylogger.info("Receive thread Started and listening to the port 25000");
			while(true) {
				msgbody = null;
				Socket socket = serverSocket.accept();
				InputStream is = socket.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				readmsg = br.readLine();
				while (readmsg != null) {
					if(msgbody == null)
						msgbody = readmsg;
					else
						msgbody = msgbody+readmsg;
					readmsg = br.readLine();
				}
				ts = rcvts.getts(read_senderts(msgbody));
				mylogger.info("Receiver Thread : Received a Message, Message received Timestamp: "+ts);
				msginfo.msgbody = msgbody;
				msginfo.msg_rcvd_ts = ts;
				mylogger.info("Receiver Thread : Sent message to syncmsgs");
				syncmsg.set_msg(msginfo);
				sleep(100);
			}

		}
		catch(IOException e) {
			mylogger.info("Receive thread : Input exception\n");
			mylogger.info("----x--------x----------x------------x-----------x--------x--------");
		}catch(InterruptedException e) {
			mylogger.info("Sender Thread : Error in synchronizing threads\n");
			mylogger.info("----x--------x----------x------------x-----------x--------x--------");
		}

		}
	
	public void run() {
		m_rcv();
	}

}

class Timestamp {
	private int ts=0;
	Logger mylogger;

	public Timestamp(Logger logger) {
		mylogger = logger;
	}


	public synchronized int getts(int msgts) throws InterruptedException {
		if(msgts == 0) {  //sender thread
			ts ++;
		}
		else {
			mylogger.info("getts : msgts received :"+msgts+"previous timestamp :"+ts);
			ts = (ts > msgts ?ts:msgts)+1;
		}
		return ts;
	}
}

class RcvdMsgInfo {
	public String msgbody;
	public int msg_rcvd_ts;

	public RcvdMsgInfo() {
	}

	public RcvdMsgInfo(String msgbody, int ts) {
		this.msgbody = msgbody;
		ts = msg_rcvd_ts;
	}
}

class SyncMsgs {
	private RcvdMsgInfo rmsginfo;
	private static boolean lock=false;
	Logger mylogger;

	public SyncMsgs(Logger logger) {
		mylogger = logger;
	}


	public synchronized void set_msg(RcvdMsgInfo rmsginfo) throws InterruptedException {
		mylogger.info("set_msg : Dumping msg from receiver to syncmsg");
		this.rmsginfo = rmsginfo;
		lock = true;
		mylogger.info("set_msg : Leaving set_msg");

		
	}
	public synchronized RcvdMsgInfo get_msg() throws InterruptedException {
		if(lock) {
			mylogger.info("get_msg : Received msg from syncmsgs: ");
			mylogger.info("---------------------------------------------------------------------");
			mylogger.info(rmsginfo.msgbody);
			mylogger.info("---------------------------------------------------------------------");
			lock = false;
			return rmsginfo;
		}
		return null;
	}
}



class Node {
	public int dstatus;
	public String msgbody;
	public int finalts;
	public int senderts;
	public Node next;


	public Node(){ 
	this.next = null;
	}
	public Node(int dstatus, String msgbody, int finalts,int senderts) {
		this.dstatus  = dstatus;
		this.msgbody = msgbody;
		this.finalts = finalts;
		this.senderts = senderts;
		this.next = null;
	}
}

class Msgbuffer {
	private static Node head=null;
	Logger mylogger;

	public Msgbuffer(Logger logger) {
		mylogger = logger;
	}

	public void insert_msg(Node undmsgnode) { //undeliverable msg 
		Node temp = head;
		undmsgnode.next = null;
		mylogger.info("Inserting node in msgbuffer");
		if(head  == null)  {
			head = undmsgnode;
		}
		else {
			while(temp.next != null) {
				temp = temp.next;
			}
			temp.next = undmsgnode;
		}
	}
	public Node find_msg(int msgno) {
		int start_index=0, end_index =0,node_msgno=0;
		Node temp = head;

		while(temp != null) {
			start_index = temp.msgbody.indexOf("Msg No:");
			end_index = temp.msgbody.indexOf("Sender:");
			node_msgno = Integer.parseInt(temp.msgbody.substring(start_index+8,end_index-3));
			mylogger.info("find_msg : Node Msg no is : "+node_msgno);
			if(node_msgno == msgno) {
				mylogger.info("find_msg : Message found in msgbuffer");
				return temp;
			}
			temp = temp.next;
		}
		mylogger.info("find_msg : Message not found in msgbuffer");
		mylogger.info("------x----------x-----------x----------------x-----------x---------------");
		return null;
	}
	public void order_msgs(Node msgnode,int  msgno) { //deliverable msg
		int temp_msgno=0;
		String msg_body= null;
		Node temp=null, prev=head;
		temp = head;
		if(temp.next == null)
			return;
		firstwhileloop:
		while(temp != null) {
			msg_body = temp.msgbody;
			temp_msgno = Integer.parseInt(msg_body.substring((msg_body.indexOf("Msg No:")+8),(msg_body.indexOf("Sender:")-3)));
			if(temp_msgno == msgno) {
				if(head == temp){
					head = temp.next;
					temp.next = null;
					break firstwhileloop;
				}
				prev.next = temp.next;
				temp.next = null;
				break firstwhileloop;
			}
			prev = temp;
			temp = temp.next;
		}
		prev = head;
		temp = head;
		while(temp!=null) {
			if((temp.dstatus == 0 && temp.senderts > msgnode.senderts) ||(temp.dstatus == 1 && temp.finalts > msgnode.senderts)) {
				prev = msgnode;
				msgnode.next = temp;
				mylogger.info("order_msgs : Ordered msgs in the msgbuffer");
				print_msgbuffer();
				return;
			}
			else {
				prev = temp;
				temp = temp.next;
			}
		}
		prev = msgnode;
		msgnode.next = null;
		mylogger.info("order_msgs : Ordered msgs in the msgbuffer");
		print_msgbuffer();

	}

	public void delete_msg() {
		int flag =0,msgno=0;
		String msg_body=null;
	//Deleting the read messages i.e nodes next to head node
		while(head.dstatus == 1) {
			msg_body = head.msgbody;
			msgno = Integer.parseInt(msg_body.substring((msg_body.indexOf("Msg No:")+8),(msg_body.indexOf("Sender:")-3)));
			mylogger.info("delete_msg : msgno : "+msgno+"  was at the begining of the list and is deleted from the list now");
			mylogger.info("----------------------------------------------------------------------------");
			mylogger.info("----------------------------------------------------------------------------");
			mylogger.info("\n");
			head = head.next;
			flag ++;
			if(head == null)
				return;
		}
		if (flag > 0)
			return;
		else {
			mylogger.info("delete_msg : msg at the begining of the list is undeliverable");
			mylogger.info("delete_msg : could not delete msg from msgbuffer");
			mylogger.info("---------x-----------x----------------x-------------x----------------x---------x-------");
		}
	}
	
	public void read_msg() {
		int msgno = 0, flag =0;
		Node temp= head;
		String msg_body = null; 

		while(temp.dstatus == 1) {	
			msg_body = temp.msgbody;
			msgno = Integer.parseInt(msg_body.substring((msg_body.indexOf("Msg No:")+8),(msg_body.indexOf("Sender:")-3)));
			System.out.println("read_msg : Reading msg from msgbuffer");
			System.out.println("Received Message: ");
			System.out.println(temp.msgbody);
			System.out.println("Final Timestamp: "+temp.finalts);
			System.out.println("delivery status: "+temp.dstatus);
			System.out.println("--------------------------------------------------------------------------------------");
			System.out.println("--------------------------------------------------------------------------------------");
			System.out.println();
			mylogger.info("read_msg : Read msg from msgbuffer msg no : "+msgno+" , final ts : "+temp.finalts);
			temp = temp.next;
			flag ++;
			if(temp == null) 
				return;
		}
		if(flag > 0)
			return ;
		
		else {
			mylogger.info("read_msg : msg at the begining of the list is undeliverable");
			mylogger.info("read_msg : could not read msg from msgbuffer");
			mylogger.info("---------x-----------x----------------x-------------x----------------x---------x-------");
		}
	}
	public void print_msgbuffer() {
		Node temp = null;
		temp=head;
		mylogger.info("print_msgbuffer : printing all the msgs in the msgbuffer");
		while(temp != null) {
			mylogger.info("---------------------------------------------------------------------------");
			mylogger.info("Message: "+temp.msgbody);
			mylogger.info("Delivery Status: "+temp.dstatus);
			mylogger.info("Final Timestamp: "+temp.finalts);
			mylogger.info("next node: "+temp.next);
			mylogger.info("---------------------------------------------------------------------------");
			temp = temp.next;
		}		
		mylogger.info("print_msgbuffer : Exiting");
	}
}			

class MsgReceiver {
	public String rname;
	public int ts;

	public MsgReceiver() {
	}
}

class MsgDeliveryInfo {
	
	int msgno;
	int no_of_receivers;
	MsgReceiver r[];
	public int msgfts;
	Logger mylogger;
	
	public MsgDeliveryInfo(Logger logger) {
		mylogger = logger;
	}
	
	public MsgDeliveryInfo(int msg_no,String host[],int no_of_receivers, Logger mylogger) {
		this.msgno = msg_no;
		this.no_of_receivers = no_of_receivers;
		r =  new MsgReceiver[no_of_receivers];
		for(int i=0; i<no_of_receivers ;i++) {
			r[i] = new MsgReceiver();
			r[i].rname = host[i];
			r[i].ts = 0;
		}
		msgfts = 0;
		this.mylogger = mylogger; 
	}
}


class BookkeepingMsgs {
	static MiscellaneousWork mw;
	static int table_size;
	static List[] btable;
	Logger mylogger;
	public BookkeepingMsgs(Logger logger) {
		mylogger = logger;
	}

	public BookkeepingMsgs(MiscellaneousWork mw, Logger logger) {
		this.mw = mw;
		table_size = Integer.parseInt(mw.read_config_file("table_size")); 
		btable = new LinkedList[table_size];
		mylogger = logger;
	}
	
	public MsgDeliveryInfo get_msg_info(int msg_no)  {

		int index=0,i=0,list_size=0,r_index=0;
		MsgDeliveryInfo msg_info = null;
		index = get_table_index(msg_no);
		if(btable[index] != null) {
			list_size = (btable[index]).size();
			mylogger.info("get_msg_info listsize of the table["+index+"]: "+list_size);
			while(i < list_size) {
				msg_info =(MsgDeliveryInfo)((btable[index]).get(i));
				if(msg_info.msgno == msg_no) {
					mylogger.info("get_msg_info : found msg delivery info in bookkeeping table : ");
					mylogger.info("-----------------------------------------------------------------------------");
					mylogger.info("Msgno : "+msg_info.msgno);
					mylogger.info("Receivers : ");
					for(r_index =0; r_index<msg_info.no_of_receivers; r_index++) {
						mylogger.info("Receiver name : "+msg_info.r[r_index].rname);
						mylogger.info("ts : "+msg_info.r[r_index].ts);
					}
					mylogger.info("Msg Finalts : "+msg_info.msgfts); 
					mylogger.info("-----------------------------------------------------------------------------");
					return msg_info;
				}
				i++;
			}
		}
		mylogger.info("get_msg_info : Msg delivery info not found in the bookkeeping table");
		mylogger.info("------------x--------x----------x------------x-----------x--------x--------");
		return ((MsgDeliveryInfo)null);
	}

	public void add_new_msg_info(MsgDeliveryInfo msg_info) {
		int table_index=0;
		table_index = get_table_index(msg_info.msgno);
		if((get_msg_info(msg_info.msgno)) != null) {
			mylogger.info("add_new_msg_info : Error, duplicate Msg delivery info, Msg no already exist in table");
			mylogger.info("x----------x------------x-----------x--------x------------x-----------x--------x--------");
			return;
		}
		mylogger.info("add_new_msg_info : No duplicate msg delivery info found, all good");
		if(btable[table_index] == null)
			btable[table_index] = new LinkedList();
		((LinkedList)btable[table_index]).add(msg_info);
		mylogger.info("add_new_msg_info : added new msg delivery info to bookkkeeping table");
	}

	public int get_table_index(int msg_no) {
		return(msg_no % table_size);
	}

	public void print_bookkeeping_table() {
		MsgDeliveryInfo msg_info=null;
		int list_size =0, index =0;
		mylogger.info("print_bookkeeping_table : printing all message delivery info in the bookkeeping table");
		for(int table_index = 0; table_index <table_size;table_index++) {
			if(btable[table_index] != null) {
				list_size = (btable[table_index]).size();
				index =0;
				while(index < list_size) {
					msg_info =(MsgDeliveryInfo)((btable[table_index]).get(index));
					mylogger.info("Msg no: "+msg_info.msgno);
					mylogger.info("No. of Msg Receivers: "+msg_info.no_of_receivers);
					mylogger.info("Msg Receivers: ");
					for(int i =0; i < msg_info.no_of_receivers; i++) {
						mylogger.info(msg_info.r[i].rname+"   timestamp: "+msg_info.r[i].ts);
					}
					mylogger.info("Final Timestamp: "+msg_info.msgfts);
					mylogger.info("---------------------------------------------------------------");
					index ++;
				}
			}
		}
	}

	public void write_rcvd_tsmsg(int msg_no, String sender, int msg_ts) {
		int n = 0;
		MsgDeliveryInfo msg_dinfo = new MsgDeliveryInfo(mylogger);
		
		mylogger.info("write_rcvd_tsmsg : writing the rcvd ts in bookkeeping table for msg no : "+msg_no);
		msg_dinfo = get_msg_info(msg_no);
		if(msg_dinfo == null) {
			mylogger.info("write_rcvd_tsmsg : Error, Msg delivery info not found in bookkeeping table, msg timestamp cannot be updated in table");
			mylogger.info("------x--------x------------x-----------x----------------x-------------x----------x-------------x---------x-------");
			return;
		}
		while(n < msg_dinfo.no_of_receivers) {
			if(msg_dinfo.r[n].rname.equals(sender)) {
				msg_dinfo.r[n].ts = msg_ts;
				mylogger.info("write_rcvd_tsmg : Msg Timestamp updated in the bookkeeping table for receiver: "+msg_dinfo.r[n].rname);
				mylogger.info("write_rcvd_tsmg : updated ts is: "+msg_dinfo.r[n].ts);
				return;
			}
			n++;
		}
		mylogger.info("write_rcvd_tsmsg : Error message receiver not found in msg delivery info, msg timestamp could not updated");
		mylogger.info("------x--------x------------x-----------x----------------x-------------x----------x-------------x---------x-------");
	}



	public boolean is_all_ts_rcvd(int msgno) {
		int n = 0;
		MsgDeliveryInfo msg_dinfo = new MsgDeliveryInfo(mylogger);

		msg_dinfo = get_msg_info(msgno);
		if(msg_dinfo == null) {
			mylogger.info("is_all_ts_rcvd : Error, Msg delivery info not found in bookkeeping table");
			mylogger.info("is_all_ts_rcvd : could not find if all ts for msgno : "+msgno+" is received or not");
			mylogger.info("------x--------x------------x-----------x----------------x-------------x----------x-------------x---------x-------");
			return false;
		}
		mylogger.info("is_all_ts_rcvd : finding if all ts is received or not");
		while(n < msg_dinfo.no_of_receivers) {
			if(msg_dinfo.r[n].ts == 0) {
				mylogger.info("is_all_ts_rcvd : Receiver : "+msg_dinfo.r[n].rname+" has not send ts for msgno : "+msgno);
				return false;
			}
			n++;
		}
		mylogger.info("is_all_ts_rcvd : All receivers of msgno : "+msgno+" have send their timestamps");
		return true;
	}
	
	public int find_and_write_fts(int msgno) {
		int n = 0, max_ts =0;
		MsgDeliveryInfo msg_dinfo = new MsgDeliveryInfo(mylogger);

		msg_dinfo = get_msg_info(msgno);
		if(msg_dinfo == null) {
			mylogger.info("find_and_write_fts : Error, Msg delivery info not found in bookkeeping table");
			mylogger.info("find_and_write_fts : final timestamp of msgno: "+msgno+" could not be updated in the table");
			mylogger.info("------x--------x------------x-----------x----------------x-------------x----------x-------------x---------x-------");
			return 0;
		}
		while(n < msg_dinfo.no_of_receivers) {
			if(msg_dinfo.r[n].ts > max_ts)
				max_ts = msg_dinfo.r[n].ts;
			n++;
		}
		msg_dinfo.msgfts = max_ts;
		mylogger.info("find_and_write_fts: Updated msgno : "+msgno+" with final ts : "+max_ts); 
		return max_ts;
	}

}
class MsgOrdering extends Thread {
	Msgbuffer msgbuf;
	Thread sndmsg;
	SyncMsgs syncmsg;
	BookkeepingMsgs bmsgs;
	String mymachinename;
	Logger mylogger;

	public MsgOrdering(Thread sndmsg,Msgbuffer msgbuf, SyncMsgs syncmsg, BookkeepingMsgs bmsgs, String mymachinename, Logger logger) {
		this.msgbuf = msgbuf;
		this.sndmsg = sndmsg;
		this.syncmsg = syncmsg;
		this.bmsgs = bmsgs;
		this.mymachinename = mymachinename;
		mylogger = logger;
	}


	public void run () {
		RcvdMsgInfo rmsginfo = null;
		try {
			while(true) {
				rmsginfo = syncmsg.get_msg();
				if(rmsginfo != null) {
					mylogger.info("msgordering thread : Received message from syncmsgs ");
					mylogger.info("msgordering thread : now sending it for processing");
					process_rcvdmsg(rmsginfo.msgbody, rmsginfo.msg_rcvd_ts);

				}
				
			}
			
		}catch(InterruptedException e) {
			mylogger.info("MsgOrdering Thread : Failed in accesing the shared object");
		}

	}
	public void send_ts(int msgno,String hostname,int rcvdts) {
		int n=1,current_ts =0;
		String tsmsg=null;
		String tshost[] = new String [1];
		tshost[0] = hostname;
		tsmsg = "Msg Type: Timestamp Msg ||\n"+"Msg No: "+msgno+" ||\nSender: "+mymachinename+" ||\nSender's Msg Rcvd Timestamp: "+rcvdts;
		try {
			mylogger.info("send_ts : asked sender to send the ts msg");
			current_ts = ((Sender)sndmsg).sendts.getts(0);
			((Sender)sndmsg).m_send(tshost, tsmsg, n);
			mylogger.info("send_ts : sent timestamp msg for msgno: "+msgno+" to host : "+hostname);

		}catch(InterruptedException e) {
			mylogger.info("send_ts : Interrupted while sending request to send thread for sending ts msg");
			mylogger.info("--x---------x-----------x------------x------------x-------------x-------------x-----");
		}
	}
	public void send_fts(int msgno, int fts) {
		int i=0;
		String msg_receivers[]=null,ftsmsg=null;
		MsgDeliveryInfo msg_dinfo = null;
		
		msg_dinfo = bmsgs.get_msg_info(msgno);
		if(msg_dinfo != null) {
			msg_receivers = new String[msg_dinfo.no_of_receivers];
			for(i =0; i < msg_dinfo.no_of_receivers;i++) 
				msg_receivers[i] = msg_dinfo.r[i].rname;
			mylogger.info("send_fts : asked sender to send the fts msg");
			try {
				if(msgno == 201)
					fts = 1;
				ftsmsg = "Msg Type: Final Timestamp Msg ||\n"+"Msg No: "+msgno+
				" ||\nSender: "+mymachinename+" ||\nFinal Timestamp: "+fts+" ||\nSender Timestamp: "+((Sender)sndmsg).sendts.getts(0);
				((Sender)sndmsg).m_send(msg_receivers, ftsmsg, msg_dinfo.no_of_receivers);
				mylogger.info("send_fts : sent final timestamp msg for msgno: "+msgno+" fts: "+fts);
				mylogger.info("--------------------------------------------------------------------------------------");
				mylogger.info("--------------------------------------------------------------------------------------");
				System.out.println("--------------------------------------------------------------------------------------");
				System.out.println("--------------------------------------------------------------------------------------");
				System.out.println();
			}catch(InterruptedException e) {
				mylogger.info("send_fts: Interrupted while sending request to send thread for sending fts msg");
				mylogger.info("--x---------x-----------x------------x------------x-------------x-------------x-----");
			}
		}
	}

	public void process_rcvdmsg(String msgbody, int ts){
		int fts=0,msgts=0,sname_index=0,msgno=0,msgfts =0;
		String msg_type=null,sender=null,temp=null,temp_str=null;
		Node msgnode=null;
		
		sname_index = msgbody.indexOf("Sender");
		sender = msgbody.substring(sname_index+8,sname_index+13);
		
		temp_str = msgbody.substring(msgbody.lastIndexOf(':')+2);
		msgts = Integer.parseInt(temp_str);
		
		temp = msgbody.substring((msgbody.indexOf("Msg No:"))+8,(msgbody.indexOf("Sender:"))-3);
		msgno = Integer.parseInt(temp);

		msg_type = msgbody.substring(10,(msgbody.indexOf("Msg No:"))-3);
		mylogger.info("process_rcvdmsg : Extracted details from the message");
		mylogger.info("process_rcvdmsg : Msgtype : "+msg_type+" Msgno : "+msgno+" sender: "+sender);
		


		if(msg_type.equals("New Msg")) {
			Node rcvdmsgnode= new Node(0,msgbody,0,msgts);
			mylogger.info("process_rcvdmsg : Received New Msg and send to msgbuffer for buffering it, msg no: "+msgno);
			msgbuf.insert_msg(rcvdmsgnode); //msg inserted in buffer but is undeliverable right now
			mylogger.info("process_rcvdmsg : sending a ts msg ");
			send_ts(msgno,sender,ts);
		}
		else if(msg_type.equals("Timestamp Msg")) {			
			System.out.println("Received a Timestamp Message : ");
			System.out.println(msgbody);
			System.out.println("----------------------------------------------------------------------------");
			mylogger.info("process_rcvdmsg : Received Timestamp message, msg no: "+msgno+" , Sender timestamp is : "+msgts);
			bmsgs.write_rcvd_tsmsg(msgno, sender, msgts); // bookkeeping write in file
			if(bmsgs.is_all_ts_rcvd(msgno)){
				mylogger.info("process_rcvdmsg : all timestamps for msgno : "+msgno+" is received");
				mylogger.info("process_rcvdmsg : finding final timestamp for msgno : "+msgno);
				fts = bmsgs.find_and_write_fts(msgno);
				if(fts == 0) {
					mylogger.info("process_rcvdmsg : found Final timestamp = 0, wrong final timestamp");
					mylogger.info("--x---------x-----------x------------x------------x-------------x-------------x-----");
					return;
				}
				mylogger.info("process_rcvdmsg : sending a fts msg for msgno : "+msgno);
				send_fts(msgno,fts);
			}
		}
		else if (msg_type.equals("Final Timestamp Msg")) {
			temp_str = msgbody.substring((msgbody.indexOf("Final Timestamp:"))+17, (msgbody.indexOf("Sender Timestamp: "))-3);
			msgfts = Integer.parseInt(temp_str);
			mylogger.info("process_rcvdmsg : Received Final Timestamp message, msg no: "+msgno+" final timestamp is : "+msgfts);
			msgnode = msgbuf.find_msg(msgno); 	
			if(msgnode == null) {
				mylogger.info("process_rcvdmsg : could not proceed, msgno : "+msgno+" not found in the msgbuffer");
				mylogger.info("--x---------x-----------x------------x------------x-------------x-------------x-----");
				return;
			}
			msgnode.dstatus = 1; //msg is deliverable now
			msgnode.finalts = msgfts;
			mylogger.info("process_rcvdmsg : updated the delivery status of msgno : "+msgno+" in the msgbuffer");
			mylogger.info("process_rcvdmsg : ordering the msgs in the msgbuffer");
			msgbuf.order_msgs(msgnode,msgno);
			msgbuf.read_msg();
			msgbuf.delete_msg();
		}	
		else {
			mylogger.info("process_rcvdmsg : Wrong Msg type in msgbody");
			mylogger.info("--x---------x-----------x------------x------------x-------------x-------------x-----");
		}
	}
}
class logging extends SimpleFormatter {

	public String format(LogRecord record){
		if(record.getLevel() == Level.INFO){
			return record.getMessage() + "\r\n";
		}else{
			return super.format(record);
		}
	}
}

class MiscellaneousWork {
	File config_file;
	Logger mylogger;

	public MiscellaneousWork (File file) {
		config_file = file;
	}

	public String read_config_file(String search_string) {
		String currentLine = null;
		try {
			Scanner scanner = new Scanner(config_file);
			while(scanner.hasNextLine()) {
				currentLine = scanner.nextLine();
				if(currentLine.indexOf(search_string) >= 0) {
					mylogger.info("read_config_file : "+search_string+" is : "+currentLine);
					return (currentLine.substring(currentLine.indexOf(':')+2));
				}
			}
			mylogger.info("read_config_file : "+search_string+" not found in config file");
			scanner.close();
		}catch(FileNotFoundException e)  {
			e.printStackTrace();
		}
		return null;
	}
	public Logger set_logging(String mymachinename) {
		mylogger = Logger.getLogger("MyLog");
		FileHandler fh;
		try {
			fh = new FileHandler("logfile_"+mymachinename+".log");
			mylogger.addHandler(fh);
			logging formatter = new logging();
			fh.setFormatter(formatter);
			mylogger.setUseParentHandlers(false);
		}catch (IOException e) {
			e.printStackTrace();
		}
		return mylogger;
	}

}

public class TotalOrdering {

	public static void main(String args[]) {
		Logger mylogger;
		String mymachinename = args[0]; //pass system name at the time of execution	
		File config_file = new File("config.txt");
		MiscellaneousWork mw = new MiscellaneousWork(config_file);
		mylogger = mw.set_logging(mymachinename);
		Timestamp tstamp = new Timestamp(mylogger);
		Msgbuffer msgbuf = new Msgbuffer(mylogger);
		SyncMsgs syncmsg = new SyncMsgs(mylogger);
		BookkeepingMsgs bmsgs = new BookkeepingMsgs(mw,mylogger);
		Thread sndmsg = new Sender(tstamp, mymachinename,mw, mylogger);
		Thread rcvmsg= new Receiver(tstamp,syncmsg,mw, mylogger);
		sndmsg.start();
		rcvmsg.start();
		Thread ordermsg = new MsgOrdering(sndmsg,msgbuf,syncmsg,bmsgs,mymachinename, mylogger);
		ordermsg.start();
		try {
			sndmsg.join();
			rcvmsg.join();
			ordermsg.join();
			} catch (InterruptedException e) {
				mylogger.info("Problem in joining the threads\n");
			}
		}

	}
