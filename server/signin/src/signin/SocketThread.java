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
					outputStream.writeInt(num);
				}
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
					outputStream.writeInt(num);
				}
			}
			else if(cmdkind.equals("setClass")){
				String className=inputStream.readUTF();
				int teacherNum=inputStream.readInt();
				int timeLimit=inputStream.readInt();
				
				//search if the class name has been used
				String checksql="select * from classInfo where name='"+className+"'";
				resultSet=	DBUtils.select(connection, checksql);
				if(resultSet.next()){
					//the class name already exists
					outputStream.writeInt(-2);
					outputStream.close();
					inputStream.close();
					socket.close();
					return;
				}
				
				int num=0;
				String numsql="select max(num) from classInfo";
				resultSet=DBUtils.select(connection, numsql);
				if(resultSet.next()){
					num=resultSet.getInt(1);
				}
				num++;
				//create class sql
				String sql="insert into classInfo values("+num+",'"+className+"',"+timeLimit+","+teacherNum+")";
				int result=DBUtils.insert(connection, sql);
				if(result==-1){
					outputStream.writeInt(-1);
				}
				else{
					//create a signin info table for this class 
					String classTable="create table "+num+"(stuNum int,siginTime String)";
					int createClassTable=DBUtils.createTable(connection,classTable);
					if(createClassTable==-1){
						//delete this class info from the classInfo table
						String deleteSql="delete from classInfo where name='"+className+"'";
						DBUtils.select(connection, deleteSql);
						outputStream.writeInt(-1);
					}
					else{
						//successfully created
						outputStream.writeInt(1);
						//return the class number
						outputStream.writeInt(num);
					}
				}
			}
			else if(cmdkind.equals("startSignin")){
				
			}
			else if(cmdkind.equals("signin")){
				
			}
			inputStream.close();
			outputStream.close();
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

}
