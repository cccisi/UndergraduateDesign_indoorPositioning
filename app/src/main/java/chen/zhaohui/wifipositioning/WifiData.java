package chen.zhaohui.wifipositioning;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
/**
 * Created by 陈朝晖 on 2017/5/10.
 */


public class WifiData {

//HandlerThread thread = new HandlerThread("NetWork");
//thread.start();
//	Handler handler = new Handler(thread.getLooper());


	private static Connection getConn() {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		String driver = Config.DRIVER;                     //连接数据库所使用的JAR包资源路径
		String url = "jdbc:jtds:sqlserver://192.168.24.1:1433/WiFi";        //连接数据库URL
		String user = Config.USER;                         //数据库用户名
		String pass = Config.PASS;

		Connection conn = null;
		try {
			Class.forName(driver); //classLoader,加载对应驱动
			conn = (Connection) DriverManager.getConnection(url, user, pass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return conn;
	}
//		String[][] Res_aqjc;                     //存储安全监测数据的对象
//		List<String[]> aqjcListView;
//		ListView listView;                    //返回查询结果的listView
//		Button searchBtn;              //查询按钮
//		AlertDialog.Builder searchDialog;      //查询对话框
//
//		Connection con = null;
//		Statement stmt = null;
//		ResultSet rs = null;
//		String reStr;
//		try {
//			Class.forName(driver);
//			con = DriverManager.getConnection(url, user, pass);
//			stmt = con.createStatement();
//
//			reStr = "select *";
//			rs = stmt.executeQuery(reStr);
//			//将查询数据加载到集合中
//			int resRow = 0;
//			while (rs.next()) {
//				++resRow;
//			}
//			Res_aqjc = new String[resRow][8];
//			rs = stmt.executeQuery(reStr);
//			int i = 0;
//			while (rs.next()) {
//				for (int j = 1; j < 9; j++) {
//					Res_aqjc[i][j - 1] = rs.getString(j);
//				}
//				i++;
//			}
//
//			aqjcListView = new ArrayList<String[]>();
//			for (int index = 0; index < Res_aqjc.length; index++) {
//				aqjcListView.add(Res_aqjc[index]);
//			}
//
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SQLException e)
//
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}


	// preference key
	public static final String keyGridUnit = "pref_grid_unit";
	public static final String keyGridSize = "pref_grid_size";

	// default value for floor plan
	public static final int GRID_WIDTH_INIT = 200;
	public static final int GRID_UNIT_INIT = 3;
	public static final int MIN_GRID_UNIT = 1;
	public static final int MIN_GRID_SIZE = 50;
	public static final int MAX_GRID_SIZE = 500;
	// parameters for WiFi scanning
	public static final float WIFI_MAX_RSSI = 100;
	public static final int NUM_WIFI_CATPURE = 10;//scan10次取平均值
	public static final long WIFI_SURVEY_INTERVAL = 5000;//survey间隔5秒
	public static final long WIFI_SCAN_INTERVAL = 2000;  //scan间隔2秒
	// parameters for K-Means
	public static final int KMEANS_NUM_PT_POS = 1;
	// parameters for Orientation Filter
	public static final float OF_HALF_ANGLE = 60;
	// parameters for Trust Region
	public static final float TR_INIT_RADIUS = (float) 2.0;
	public static final float TR_MAX_RADIUS = (float) 20.0;
	public static final float TR_MIN_RADIUS = (float) 2.0;
	public static final float TR_LOW_RATIO = (float) 0.3;
	public static final float TR_HIGH_RATIO = (float) 0.95;
	public static final float TR_SHINK_RATIO = (float) 0.7;
	public static final float TR_GROW_RATIO = (float) 1.5;
	public static final float TR_ADJUST_RATIO = (float) 3.0;
	
	// single instance object
	private static WifiData instance = null;
	
	// member variable
	protected Activity activity;
	protected SharedPreferences sp = null;
	protected Bitmap imgFloorplan, imgRedDot, imgGreenDot, imgBlueDot, imgOrangeDot;
	protected int lastPointId = 0;
	protected WifiDBHelper wHelper;
	protected SQLiteDatabase wDB;
	protected DecimalFormat cf;
	protected SimpleDateFormat df;

	public class Point {
		public int pid;
		public float px;
		public float py;
		public int cnt_survey;
	}
	
	public class Survey {
		public int sid;
		public int pid;
		public Date sts;
		public int cnt_sample;
	}

//sid：sample ID；pid：pointID（少）一个point可以有多个sample
	public class Sample {
		public int sid;
		public int pid;
		public String bssid;
		public String ssid;
		public float rssi;
		public int cnt_rssi;
		public int freq;
		public String desc;
	}
	
	public class PositionSurvey {
		public float px;
		public float py;
		public Map<String, Float> mapSample = new HashMap<String, Float>();
	}
	
	public class PositionTemp {
		public float px;
		public float pxScaled;
		public float py;
		public float pyScaled;
		public float pAdjX;
		public float pAdjY;
		public float radius = TR_INIT_RADIUS;
		public float degree;
		public float rssiDiff;
		public float rssiDiffTotal;
	}
	
	protected WifiData(Activity act) {
		activity = act;
		sp = PreferenceManager.getDefaultSharedPreferences(activity);
		wHelper = new WifiDBHelper(act);
		wDB = wHelper.getWritableDatabase();
        cf = new DecimalFormat("#.#");
        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}
	
	public static WifiData getInstance(Activity act) {
		if (instance == null)
			instance = new WifiData(act);
		return instance;
	}
	
	public int getGridUnit() {
		return sp.getInt(keyGridUnit, GRID_UNIT_INIT);
	}
	
	public int getGridSize() {
		return sp.getInt(keyGridSize, GRID_WIDTH_INIT);
	}
	
	public void setGrid(int size, int unit) {
		SharedPreferences.Editor ed = sp.edit();
		ed.putInt(keyGridUnit, unit);
		ed.putInt(keyGridSize, size);
		ed.commit();
	}
	
	public Bitmap getFloorplanImage() {
		if (imgFloorplan == null)
			imgFloorplan = ((BitmapDrawable)activity.getResources().getDrawable(R.drawable.floorplan)).getBitmap();
		return imgFloorplan;
	}
	
	public Bitmap getImgRedDot() {
		if (imgRedDot == null)
			imgRedDot = ((BitmapDrawable)activity.getResources().getDrawable(R.drawable.reddot)).getBitmap();
		return imgRedDot;
	}

	public Bitmap getImgBlueDot() {
		if (imgBlueDot == null)
			imgBlueDot = ((BitmapDrawable)activity.getResources().getDrawable(R.drawable.bluedot)).getBitmap();
		return imgBlueDot;
	}
	
	public Bitmap getImgGreenDot() {
		if (imgGreenDot == null)
			imgGreenDot = ((BitmapDrawable)activity.getResources().getDrawable(R.drawable.greendot)).getBitmap();
		return imgGreenDot;
	}
	
	public Bitmap getImgOrangeDot() {
		if (imgOrangeDot == null)
			imgOrangeDot = ((BitmapDrawable)activity.getResources().getDrawable(R.drawable.orangedot)).getBitmap();
		return imgOrangeDot;
	}
	
	public String formatDecimal(float f) {
		return cf.format(f);
	}
	
	public String formatDatetime(Date d) {
		return df.format(d);
	}

	// SQLite functions / objects
	public Point newPointObj() {
		return new Point();
	}
	
	public Survey newSurveyObj() {
		return new Survey();
	}
	
	public Sample newSampleObj() {
		return new Sample();
	}
	
	public PositionTemp newPositionTemp() {
		return new PositionTemp();
	}

	public void reFresh(){
		wDB.execSQL("DELETE FROM "+WifiDBHelper.TABLE_SAMPLE);
		wDB.execSQL("DELETE FROM "+WifiDBHelper.TABLE_SURVEY);
		wDB.execSQL("DELETE FROM "+WifiDBHelper.TABLE_POINT);
		wDB.execSQL("DELETE FROM sqlite_sequence");
		Log.e("WiFiData", "reFresh: 已经清空SQLite" );
		Connection conn = getConn();
		String reFreshPoint = "select * from WiFi.dbo.WPoint";
		PreparedStatement pstmt_reFreshPoint;
		try {
			pstmt_reFreshPoint = (PreparedStatement)conn.prepareStatement(reFreshPoint);
			ResultSet rs_reFreshPoint = pstmt_reFreshPoint.executeQuery();
//			ResultSetMetaData md = rs_reFreshPoint.getMetaData();
//			int col = md.getColumnCount();// 获取列数
			//List<ContentValues>传数据
			wDB.beginTransaction(); // 手动设置开始事务
			while (rs_reFreshPoint.next()) {
				ContentValues dataFromSQLserver = null;
				dataFromSQLserver = new ContentValues();//对象赋值
				for (int i = 1; i <= 2; i++) {
					int px = rs_reFreshPoint.getInt(2);
					int py = rs_reFreshPoint.getInt(3);
					dataFromSQLserver.put("px",px);
					dataFromSQLserver.put("py",py);

				}//用forEach取值
				wDB.insert(WifiDBHelper.TABLE_POINT, null, dataFromSQLserver);
//				wDB.setTransactionSuccessful(); // 设置事务处理成功，不设置会自动回滚不提交



			}
		} catch (SQLException e) {
			e.printStackTrace();
		}//WPoint更新
		Log.e("WiFiData", "reFresh: 已经WPoint更新" );


		String reFreshSurvey = "select * from WiFi.dbo.WSurvey";
		PreparedStatement pstmt_reFreshSurvey;
		try {
			pstmt_reFreshSurvey = (PreparedStatement)conn.prepareStatement(reFreshSurvey);
			ResultSet rs_reFreshSurvey = pstmt_reFreshSurvey.executeQuery();
//			ResultSetMetaData md_Survey = rs_reFreshSurvey.getMetaData();
//			int col = md_Survey.getColumnCount();// 获取列数
			//List<ContentValues>传数据

			while (rs_reFreshSurvey.next()) {
				ContentValues dataFromSQLserver = null;
				dataFromSQLserver = new ContentValues();//对象赋值
				for (int i = 1; i <= 2; i++) {
					int pid = rs_reFreshSurvey.getInt(2);
					String sts = rs_reFreshSurvey.getString(3);
					dataFromSQLserver.put("pid",pid);
					dataFromSQLserver.put("sts",formatDatetime(new Date()));

				}//用forEach取值
				wDB.insert(WifiDBHelper.TABLE_SURVEY, null, dataFromSQLserver);
//				wDB.setTransactionSuccessful(); // 设置事务处理成功，不设置会自动回滚不提交



			}
		} catch (SQLException e) {
			e.printStackTrace();
		}//WSurvey更新
		Log.e("WiFiData", "reFresh: 已经WSurvey更新" );

		String reFreshSample = "select * from WiFi.dbo.WSample";
		PreparedStatement pstmt_reFreshSample;
		try {
			pstmt_reFreshSample = (PreparedStatement)conn.prepareStatement(reFreshSample);
			ResultSet rs_reFreshSample = pstmt_reFreshSample.executeQuery();
//			ResultSetMetaData md_Sample = rs_reFreshSample.getMetaData();
//			int col = md.getColumnCount();// 获取列数
			//List<ContentValues>传数据

			while (rs_reFreshSample.next()) {
				ContentValues dataFromSQLserver = null;
				dataFromSQLserver = new ContentValues();//对象赋值
				for (int i = 1; i <= 7; i++) {
					int pid = rs_reFreshSample.getInt(1);
					int sid = rs_reFreshSample.getInt(2);
					String bssid = AES.decrypt(rs_reFreshSample.getString(3));
//					AES解密
					String ssid = AES.decrypt(rs_reFreshSample.getString(4));
//                    String ssid = rs_reFreshSample.getString(4);
					Float rssi = rs_reFreshSample.getFloat(5);
					int freq = rs_reFreshSample.getInt(6);
					String desc = AES.decrypt(rs_reFreshSample.getString(7));
					dataFromSQLserver.put("pid",pid);
					dataFromSQLserver.put("sid",sid);
					dataFromSQLserver.put("bssid", bssid);
					dataFromSQLserver.put("ssid", ssid);
					dataFromSQLserver.put("rssi", rssi);
					dataFromSQLserver.put("freq", freq);
					dataFromSQLserver.put("desc", desc);
//					Log.e("WiFiData", "reFresh: WSample写入成功" );

				}//用forEach取值
				wDB.insert(WifiDBHelper.TABLE_SAMPLE, null, dataFromSQLserver);
//				wDB.setTransactionSuccessful(); // 设置事务处理成功，不设置会自动回滚不提交
//				wDB.endTransaction(); // 处理完成


			}

			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}//WSample更新

		Log.e("WiFiData", "reFresh: 已经WSample更新" );
	}


	//若之前没有此point，新增point
	public int insertPoint(float px, float py) {
		Connection conn = getConn();
		int i = 0;
		String insertPoint = "insert into WiFi.dbo.WPoint (px,py) values (?,?)";
		PreparedStatement pstmt_insertPoint;
		try {
			pstmt_insertPoint = (PreparedStatement) conn.prepareStatement(insertPoint);
			pstmt_insertPoint.setFloat(1, px);
			pstmt_insertPoint.setFloat(2, py);
			i = pstmt_insertPoint.executeUpdate();
			pstmt_insertPoint.close();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		Connection conn_pidNumber = getConn();
		PreparedStatement pstmt_pidNumber;
		String pidNumber = "SELECT * FROM WiFi.dbo.WPoint;";
		try {
			pstmt_pidNumber = (PreparedStatement) conn_pidNumber.prepareStatement(pidNumber);
			ResultSet respidNumber = pstmt_pidNumber.executeQuery();
			while(respidNumber.next()){
				i = respidNumber.getRow();
				Log.e("WifiData", "get数量"+i);
			}
			conn_pidNumber.close();
			pstmt_pidNumber.close();
			respidNumber.close();

		} catch (Exception e) {
			// TODO: handle exception
		}

//		reFresh();
		//更新数据库
		Log.e("WiFiData", "更新数据库" );

		return i;//决定能否进入下一步

//		ContentValues values = new ContentValues();
//        values.put("px", px);
//        values.put("py", py);

//        return (int)wDB.insert(WifiDBHelper.TABLE_POINT, null, values);
	}

	public ArrayList<Point> getPoints() {
//		instance.reFresh();
		String sqlCnt = "SELECT COUNT(*) FROM " + WifiDBHelper.TABLE_SURVEY
				+ " s WHERE s.pid=p.pid";
		Cursor cursor = wDB.rawQuery("SELECT pid,px,py,(" + sqlCnt
				+ ") AS cnt_survey FROM " + WifiDBHelper.TABLE_POINT + " p",
				null);
		Log.d("WifiData", "getPoints # of points="+cursor.getCount());
		cursor.moveToFirst();
		ArrayList<Point> pointList = new ArrayList<Point>();
		while (!cursor.isAfterLast()) {
			Point p = new Point();
			p.pid = cursor.getInt(0);
			p.px = cursor.getFloat(1);
			p.py = cursor.getFloat(2);
			p.cnt_survey = cursor.getInt(3);
			Log.d("WifiData", "getPoints ID="+p.pid+", cnt="+p.cnt_survey);
			
			pointList.add(p);
			cursor.moveToNext();
		}
		cursor.close();
		return pointList;
	}
	
	public Point getPoint(int pid) {
		String sqlCnt = "SELECT COUNT(*) FROM " + WifiDBHelper.TABLE_SURVEY
				+ " s WHERE s.pid=p.pid";
		Cursor cursor = wDB.rawQuery("SELECT pid,px,py,(" + sqlCnt
				+ ") AS cnt_survey FROM " + WifiDBHelper.TABLE_POINT + " p WHERE p.pid="+pid,
				null);
		cursor.moveToFirst();
		Point p = new Point();
		while (!cursor.isAfterLast()) {
			p.pid = cursor.getInt(0);
			p.px = cursor.getFloat(1);
			p.py = cursor.getFloat(2);
			p.cnt_survey = cursor.getInt(3);
			cursor.moveToNext();
		}
		cursor.close();
		return p;
	}

	public void removePoint(int pid) {
		Connection conn = getConn();
		int i = 0;
		String removePoint = "DELETE FROM "+WifiDBHelper.TABLE_SAMPLE+" WHERE pid="+pid
							+"DELETE FROM "+WifiDBHelper.TABLE_SURVEY+" WHERE pid="+pid
							+"DELETE FROM "+WifiDBHelper.TABLE_POINT+" WHERE pid="+pid;
		PreparedStatement pstmt_removePoint;
		try {
			pstmt_removePoint = (PreparedStatement) conn.prepareStatement(removePoint);
			i = pstmt_removePoint.executeUpdate();
			Log.e("WiFiData", "发生了WPoint删除"+pid );
			pstmt_removePoint.close();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}


//		wDB.execSQL("DELETE FROM "+WifiDBHelper.TABLE_SAMPLE+" WHERE pid="+pid);
//		wDB.execSQL("DELETE FROM "+WifiDBHelper.TABLE_SURVEY+" WHERE pid="+pid);
//		wDB.execSQL("DELETE FROM "+WifiDBHelper.TABLE_POINT+" WHERE pid="+pid);
	}
//	private static DateFormat format2= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//	public static Date StringToDate(String datestr){
//		Date date = null;
//		try {
//			date = format2.parse(datestr);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return date;
//	}
//
//	public static String DateToString(Date date){
//		try {
//			return format2.format(date);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return "";
//	}


	public int insertSurvey(int pid, ArrayList<Sample> listSample) {
		Connection conn_insertSurvey = getConn();

		int i = 0;
		int sid = 0;
		String insertSurvey = "insert into WiFi.dbo.WSurvey (pid,sts) values (?,?)";
		PreparedStatement pstmt_insertSurvey;
		try {
			pstmt_insertSurvey = (PreparedStatement) conn_insertSurvey.prepareStatement(insertSurvey);
			pstmt_insertSurvey.setFloat(1, pid);
			pstmt_insertSurvey.setString(2, formatDatetime(new Date()));
			i = pstmt_insertSurvey.executeUpdate();
			pstmt_insertSurvey.close();
			conn_insertSurvey.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}


		Connection conn_sidNumber = getConn();
		PreparedStatement pstmt_sidNumber;
		String sidNumber = "SELECT * FROM WiFi.dbo.WSurvey;";
		try {
			pstmt_sidNumber = (PreparedStatement) conn_sidNumber.prepareStatement(sidNumber);
			ResultSet ressidNumber = pstmt_sidNumber.executeQuery();
			while(ressidNumber.next()){
				sid = ressidNumber.getRow();
				Log.e("WifiData", "get数量"+sid);
			}
//		Connection conn = getConn();
//		String sidNumber = "SELECT * FROM WiFi.dbo.WSurvey;";
//		try {
//			Statement stasidNumber = conn.createStatement();
//			ResultSet respidNumber = stasidNumber.executeQuery(sidNumber);
//			while(respidNumber.next()){
//				sid = respidNumber.getInt(1);
//			}
			pstmt_sidNumber.close();
			ressidNumber.close();
			conn_sidNumber.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		Log.d("Sample","sid="+sid);


		Log.d("Sample","insertSurvey");
		Connection conn_insertSample = getConn();
		if (i != -1) {
			String insertSample = "insert into WiFi.dbo.WSample (pid,sid,bssid,ssid,rssi,freq,des) values (?,?,?,?,?,?,?)";
			PreparedStatement pstmt_insertSample;
			try {
				pstmt_insertSample = (PreparedStatement) conn_insertSample.prepareStatement(insertSample);
				for (Sample s : listSample) {
					pstmt_insertSample.setInt(1, pid);
					pstmt_insertSample.setInt(2, sid);
					pstmt_insertSample.setString(3,AES.encrypt(s.bssid));
					//	AES加密
					pstmt_insertSample.setString(4,AES.encrypt(s.ssid));
//                    pstmt_insertSample.setString(4,s.ssid);
					pstmt_insertSample.setFloat(5,s.rssi);
//					pstmt_insertSample.setInt(6,s.cnt_rssi);
					pstmt_insertSample.setInt(6,s.freq);
					pstmt_insertSample.setString(7,AES.encrypt(s.desc));
					Log.d("Sample","数据");
					i = pstmt_insertSample.executeUpdate();
				}

				pstmt_insertSample.close();
				conn_insertSample.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
//		reFresh();
//		//更新数据库
//		Log.e("WiFiData", "更新数据库" );

//		int sid = 0;
//		ContentValues values = new ContentValues();
//        values.put("pid", pid);
//        values.put("sts", formatDatetime(new Date()));
//        sid = (int) wDB.insert(WifiDBHelper.TABLE_SURVEY, null, values);
//
//        if (sid != -1) {
//    		for (Sample s : listSample) {
//    			values = new ContentValues();
//    			values.put("pid", pid);
//    			values.put("sid", sid);
//    			values.put("bssid", s.bssid);
//    			values.put("ssid", s.ssid);
//    			values.put("rssi", s.rssi);
//    			values.put("freq", s.freq);
//    			values.put("desc", s.desc);
//    			if (wDB.insert(WifiDBHelper.TABLE_SAMPLE, null, values) == -1)
//    				Log.d("WifiData", "insertSurvey: fail to insert sample pid="+pid+" sid="+sid);
//    		}
//        }
//		return sid;
		return sid;
	}
	
	public Survey getSurvey(int sid) {
		String sqlCnt = "SELECT COUNT(*) FROM " + WifiDBHelper.TABLE_SAMPLE
				+ " a WHERE a.sid=s.sid";
		Cursor cursor = wDB.rawQuery("SELECT pid,sts,(" + sqlCnt
				+ ") AS cnt_sample FROM " + WifiDBHelper.TABLE_SURVEY + " s WHERE s.sid="+sid,
				null);
		cursor.moveToFirst();
		Survey s = new Survey();
		while (!cursor.isAfterLast()) {
			s.pid = cursor.getInt(0);
			s.sid = sid;
			s.sts = Timestamp.valueOf(cursor.getString(1));
			s.cnt_sample = cursor.getInt(2);
			cursor.moveToNext();
		}
		cursor.close();
		return s;
	}
	
	public ArrayList<Survey> getSurveysByPoint(int pid) {
		String sqlCnt = "SELECT COUNT(*) FROM "+WifiDBHelper.TABLE_SAMPLE+" a WHERE a.sid=s.sid";
		Cursor cursor = wDB.rawQuery("SELECT pid,sid,sts,(" + sqlCnt
				+ ") AS cnt_sample FROM " + WifiDBHelper.TABLE_SURVEY + " s WHERE s.pid="+pid,
				null);
		Log.d("WifiData", "getSurveysByPoint pid="+pid+" # of survey="+cursor.getCount());
		
		ArrayList<Survey> surveyList = new ArrayList<Survey>();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Survey s = new Survey();
			s.pid = cursor.getInt(0);
			s.sid = cursor.getInt(1);
			s.sts = Timestamp.valueOf(cursor.getString(2));
			s.cnt_sample = cursor.getInt(3);
			surveyList.add(s);
			cursor.moveToNext();
		}
		cursor.close();
		return surveyList;
	}
	
	public void removeSurvey(int sid) {
		Connection conn = getConn();
		int i = 0;
		String removeSurvey = "DELETE FROM "+WifiDBHelper.TABLE_SAMPLE+" WHERE sid="+sid
				+"DELETE FROM "+WifiDBHelper.TABLE_SURVEY+" WHERE sid="+sid;
		PreparedStatement pstmt_removeSurvey;
		try {
			pstmt_removeSurvey = (PreparedStatement) conn.prepareStatement(removeSurvey);
			i = pstmt_removeSurvey.executeUpdate();
			System.out.println("resutl: " + i);
			pstmt_removeSurvey.close();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}


//		wDB.execSQL("DELETE FROM "+WifiDBHelper.TABLE_SAMPLE+" WHERE sid="+sid);
//		wDB.execSQL("DELETE FROM "+WifiDBHelper.TABLE_SURVEY+" WHERE sid="+sid);
	}
	
	public ArrayList<Sample> getSamplesBySurvey(int sid) {
		Cursor cursor = wDB.rawQuery("SELECT pid,bssid,ssid,rssi,freq,desc FROM "+WifiDBHelper.TABLE_SAMPLE+
				" WHERE sid="+sid, null);
		Log.d("WifiData", "getSampleBySurvey sid="+sid+" # of sample="+cursor.getCount());
		
		ArrayList<Sample> sampleList = new ArrayList<Sample>();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Sample s = new Sample();
			s.sid = sid;
			s.pid = cursor.getInt(0);
			s.bssid = cursor.getString(1);
			s.ssid = cursor.getString(2);
			s.rssi = cursor.getFloat(3);
			s.freq = cursor.getInt(4);
			s.desc = cursor.getString(5);
			sampleList.add(s);
			cursor.moveToNext();
		}
		cursor.close();
		return sampleList;
	}

	public ArrayList<PositionSurvey> getPositionSurveys() {
		ArrayList<PositionSurvey> list = new ArrayList<PositionSurvey>();
		for (Point p : getPoints()) {
			for (Survey s : getSurveysByPoint(p.pid)) {
				PositionSurvey ps = new PositionSurvey();
				ps.px = p.px;
				ps.py = p.py;
				for (Sample a : getSamplesBySurvey(s.sid))
					ps.mapSample.put(a.bssid, WIFI_MAX_RSSI + a.rssi);
				list.add(ps);
			}
		}
		return list;
	}
}
