package signin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;

public class SocketThread implements Runnable {
	
	private Socket socket;
	private Connection connection;
	private ResultSet resultSet;
	
	public SocketThread(Socket socket){
		this.socket=socket;
	}
	
    public void run() {
		try{
			connection=DBUtils.connect();
			DataInputStream inputStream=new DataInputStream(socket.getInputStream());
			DataOutputStream outputStream=new DataOutputStream(socket.getOutputStream());
			String cmdkind=inputStream.readUTF();
			
			if(cmdkind.equals("teacherRegister")){
				String name=inputStream.readUTF();
				String psw=inputStream.readUTF();
				String yesAnswer="succed";
				String noAnswer="fail";
				//get the maxest id num;
				int num=0;
				String numsql="select max(num) from teacherReg";
				resultSet=DBUtils.select(connection, numsql);
				if(resultSet.next()){
					num=resultSet.getInt(1);
				}
				//the num increases by one
				num++;
				String sql="insert into teacherReg values("+num+",'"+name+"','"+psw+"')";
				int result=DBUtils.insert(connection, sql);
				if(result==-1){
					outputStream.writeUTF(noAnswer);
				}
				else{
					outputStream.writeUTF(yesAnswer);
				}
				outputStream.writeUTF(yesAnswer);
			}
			else if(cmdkind.equals("studentRegister")){
				String name=inputStream.readUTF();
				String psw=inputStream.readUTF();
				String stuID=inputStream.readUTF();
				String stuMajor=inputStream.readUTF();
				String yesAnswer="succed";
				String noAnswer="fail";
				
				int num=0;
				String numsql="select max(num) from studentReg";
				resultSet=DBUtils.select(connection, numsql);
				if(resultSet.next()){
					num=resultSet.getInt(1);
				}
				//the num increases by one
				num++;
				//create the insert sql
				String sql="insert into studentReg values("+num+",'"+name+"','"+psw+"','"+stuID+"','"+stuMajor+"')";
				int result=DBUtils.insert(connection, sql);
				if(result==-1){
					outputStream.writeUTF(noAnswer);
				}
				else{
					outputStream.writeUTF(yesAnswer);
				}
			}
			inputStream.close();
			outputStream.close();
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

}
