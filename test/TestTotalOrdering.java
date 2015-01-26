import java.io.*;
import java.util.*;

class MsgTracking {
	public String hostname;
	public int msg_no[];
	public int rcvd_new_msg_line_no[];
	public int sent_ts_msg_line_no[];
	public int fts_rcvd_line_no[];
	public int msg_delivered_line_no[];

	public MsgTracking () {
		hostname = null;
	}
}
class OrderMsgs {
	public String hostname;
	public int msg_order[];

	public OrderMsgs() {
		hostname = null;
	}
}

class CheckTotalOrder {
	int no_of_host;
	String host[];
	int no_of_msg;
	int msg_no[];
	int msg_fts[];
	int global_order[];
	OrderMsgs order_msgs[];
	MsgTracking track_host_msgs[];

	public CheckTotalOrder(int no_of_hosts, String host[], int no_of_msg, int msgno[]) {
		int i;
		this.no_of_host= no_of_hosts;
		this.host = host;
		this.no_of_msg = no_of_msg;
		this.msg_no = msgno;
		msg_fts = new int[no_of_msg];
		order_msgs = new OrderMsgs[no_of_host+1]; //one for global order and rest per host msg order
		track_host_msgs = new MsgTracking[no_of_host];
		global_order = new int[no_of_msg];
		for(i =0; i<no_of_host; i++) {
			track_host_msgs[i] = new MsgTracking();
			order_msgs[i]= new OrderMsgs();
			track_host_msgs[i].msg_no = new int[no_of_msg];
			track_host_msgs[i].rcvd_new_msg_line_no = new int[no_of_msg];
			track_host_msgs[i].sent_ts_msg_line_no = new int[no_of_msg];
			track_host_msgs[i].fts_rcvd_line_no = new int[no_of_msg];
			track_host_msgs[i].msg_delivered_line_no = new int[no_of_msg];
			order_msgs[i].msg_order = new int[no_of_msg];
		}
		order_msgs[i] = new OrderMsgs();
		order_msgs[i].msg_order = new int[no_of_msg];
	}
	public int search_string_in_file(String search_string, File logfile, int fts_req) {
		String currentline = null, fts_str = null;
		int line_no =0;
		try {
			Scanner scanner = new Scanner(logfile);
			while(scanner.hasNextLine()) {
				currentline = scanner.nextLine();
				line_no++;
				if(currentline.indexOf(search_string) >= 0) {
					if (fts_req == 1) {
						fts_str = currentline.substring((currentline.indexOf("fts:")+5));
						return(Integer.parseInt(fts_str));
					}
					else
						return line_no;
				}
			}
			scanner.close();
		}catch(FileNotFoundException e)  {
			e.printStackTrace();
		}
		return 0;
	}

	public void update_msg_order(String hostname, int host_index) {
		int final_ts=0,j=0;
		track_host_msgs[host_index].hostname = hostname;
		File logfile = new File("logfile_"+hostname+".log");
		for(int i =0; i<no_of_msg; i++){
			track_host_msgs[host_index].msg_no[i] = msg_no[i];
			track_host_msgs[host_index].rcvd_new_msg_line_no[i] = 
				search_string_in_file("process_rcvdmsg : Received New Msg and send to msgbuffer for buffering it, msg no: "+msg_no[i], logfile, 0);
			track_host_msgs[host_index].sent_ts_msg_line_no[i] = 
				search_string_in_file("send_ts : sent timestamp msg for msgno: "+msg_no[i], logfile, 0);
			track_host_msgs[host_index].fts_rcvd_line_no[i] = 
				search_string_in_file("process_rcvdmsg : Received Final Timestamp message, msg no: "+msg_no[i], logfile, 0);
			track_host_msgs[host_index].msg_delivered_line_no[i] = 
				search_string_in_file("read_msg : Read msg from msgbuffer msg no : "+msg_no[i], logfile, 0);
			while(j < no_of_msg) {
				final_ts = search_string_in_file("send_fts : sent final timestamp msg for msgno: "+msg_no[j], logfile, 1);
				if(final_ts > 0)  //reading msg sender logfile
					msg_fts[j] = final_ts; //noting final ts from final ts msg
				j++;
			}
		}

	}

	public void track_all_msg(String hostname, int host_index) {
		for(int i =0; i<no_of_msg; i++){
			if(track_host_msgs[host_index].rcvd_new_msg_line_no[i] != 0) { // if 0 then this host is not the receiver of the msg 
				if(track_host_msgs[host_index].sent_ts_msg_line_no[i] == 0) 
					System.out.println("Error: Timestamp msg not sent for msg no : "+msg_no[i]+" by host : "+hostname);
				if(track_host_msgs[host_index].fts_rcvd_line_no[i] == 0) 
					System.out.println("Error: final timestamp msg not received for msg no : "+msg_no[i]+" by host : "+hostname);
				if(track_host_msgs[host_index].msg_delivered_line_no[i] == 0) 
					System.out.println("Error: Msg no : "+msg_no[i]+" is not read from msgbuffer by host : "+hostname);
			}
		}
	}
	public int[] find_msg_order(int msg_order[]) {
		int temp=0;
		for(int i=0; i< no_of_msg-1; i++) {
			for(int j =1; j< no_of_msg - i; j++){
				if(msg_order[j-1] > msg_order[j]) {
					temp = msg_order[j-1];
					msg_order[j-1]= msg_order[j];
					msg_order[j]=temp;
				}
			}
		}
		return msg_order;
	}
	public void find_local_msg_order(String hostname, int host_index) {
		int msg_del_order[] = new int[no_of_msg];
		order_msgs[host_index].hostname = hostname;
		for(int i =0; i <no_of_msg; i++)
			msg_del_order[i]= track_host_msgs[host_index].msg_delivered_line_no[i];
		order_msgs[host_index].msg_order = find_msg_order(msg_del_order);
		
	}
	public void find_global_msg_order() {
		int last_index = no_of_host;
		order_msgs[last_index].hostname = "Total Order";
		System.out.println();
		System.out.println("------------------------------------------------------------------------------");
		System.out.println("msg final timestapms are :");
		for(int j=0; j< no_of_msg;j++)
			System.out.println("msg no : "+msg_no[j]+"  msg final timestamp: "+msg_fts[j]);
		System.out.println("------------------------------------------------------------------------------");
		order_msgs[last_index].msg_order = find_msg_order(msg_fts);

	}
	public void print_local_msg_order(int host_index) {
		int k=0;
		int local_order[] = new int[no_of_msg];
		System.out.println("for host : "+order_msgs[host_index].hostname);
		for(int i =0; i< no_of_msg; i++) {
			if(order_msgs[host_index].msg_order[i] !=0) {
				for(int j=0; j<no_of_msg; j++) {
					if(order_msgs[host_index].msg_order[i] == track_host_msgs[host_index].msg_delivered_line_no[j]) {
						local_order[k] = msg_no[j];
						System.out.print(msg_no[j]+" ----> ");
						k++;
					}
				}
			}
		}
		System.out.println("end");
		comp_global_and_local_order(local_order, k, order_msgs[host_index].hostname);
		System.out.println("----------------------------------------------------------------------------");
	}

	public void print_global_msg_order(int original_fts_order[]) {
		int last_index = no_of_host;
		System.out.println("Total Order of msgs :");
		for(int i =0; i< no_of_msg; i++){
				innerloop:
				for(int j=0; j<no_of_msg; j++) {
					if(order_msgs[last_index].msg_order[i] == original_fts_order[j]) {
						global_order[i] = msg_no[j];
						System.out.print(msg_no[j]+" ----> ");
						original_fts_order[j] = -1;
						break innerloop;
					}
				}
			}
		System.out.println("end");
		System.out.println("----------------------------------------------------------------------------");
		System.out.println("----------------------------------------------------------------------------");

	}
	public void comp_global_and_local_order(int local_order[], int no_of_msgs_rcvd, String hostname) {
		int index_match=0, prev_match=-1;
		for(int i=0; i<no_of_msg; i++){
			for(int j=0; j<no_of_msgs_rcvd; j++) {
				if(global_order[i] == local_order[j]) {
					index_match = j;
					if(index_match < prev_match){ 
						System.out.println("host : "+hostname+" doesn't follow the global order, Violates Total Order");
						return;
					}
					prev_match = index_match;
				}
			}
		}
		System.out.println("host : "+hostname+" follows the global order, Total Order is maintained");
	
	}

}
public class TestTotalOrdering {
	public static void main(String srgs[]) {
		int no_of_host=0, no_of_msgs =0,msgno[],original_fts_order[];
		String host[];
		Scanner in = new Scanner(System.in);
		System.out.println("Enter no.of host in the system : ");
		no_of_host = Integer.parseInt(in.nextLine());
		host = new String[no_of_host];
		for(int i = 0; i< no_of_host; i++) {
			System.out.println("Enter host"+(i+1)+" name :");
			host[i] = in.nextLine();
		}
		System.out.println("Enter no. of messages exchanged in whole system :");
		no_of_msgs = Integer.parseInt(in.nextLine());
		msgno = new int[no_of_msgs];
		for(int j =0; j<no_of_msgs; j++) {
			System.out.println("Enter msg no corresponding to m"+(j+1)+" :");
			msgno[j] = Integer.parseInt(in.nextLine());
		}
		CheckTotalOrder chktotalorder = new CheckTotalOrder(no_of_host,host,no_of_msgs,msgno);
		for(int k=0; k<no_of_host;k++) {
			chktotalorder.update_msg_order(host[k], k);
			chktotalorder.track_all_msg(host[k], k);
			chktotalorder.find_local_msg_order(host[k], k);
		}
		original_fts_order =new int[no_of_msgs];
		for(int j =0; j<no_of_msgs;j++){
			original_fts_order[j] = chktotalorder.msg_fts[j];
		}
		chktotalorder.find_global_msg_order();
		chktotalorder.print_global_msg_order(original_fts_order);
		for(int k=0; k<no_of_host;k++) 
			chktotalorder.print_local_msg_order(k);
	}
}
