package signin;

import java.awt.print.Printable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class main {
	
	private static Connection connection=null;
	private static boolean tableExist=false;
	
	//����ע����Ϣ��
	private static final String createStuReg="create table studentReg(num int primary key,name nvarchar(10),psw nvarchar(20),idnum nvarchar(20),major nvarchar(40))";
	private static final String createTeaReg="create table teacherReg(num int primary key,name nvarchar(10),psw nvarchar(20))";

	public static void main(String args[]){
		//�������ݿ�
		connection=DBUtils.connect();
		tableExist=DBUtils.tableExisted(connection, "studentReg");
		if(!tableExist){
			//create tables for register
			int createstu=DBUtils.createTable(connection, createStuReg);
			if(createstu!=-1){
				System.out.println("students' register table is created");
			}
			int createtea=DBUtils.createTable(connection, createTeaReg);
			if(createtea!=-1){
				System.out.println("teachers' register table is created");
			}
		}
		
		//start the server
		Server server=new Server();
		server.startServer();
	}
}
