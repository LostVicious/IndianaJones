package indianaJones;
import indianaJones.KDTree.Euclidean;
import indianaJones.KDTree.SearchResult;

import java.awt.image.CropImageFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.UnsupportedAudioFileException;



public class IndianaJones {
	
	static private Connection conn;
	DateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	public static void musichetta() {
		File f = new File("indiana.wav");
		System.out.println(f.getAbsolutePath());
		
		AudioInputStream audioIn;
		try {
			audioIn = AudioSystem.getAudioInputStream(new File("indiana.wav"));
			Clip clip = AudioSystem.getClip();
			clip.open(audioIn);
			clip.start();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		
		Evaluation e = new Evaluation();
		musichetta();
		/*for (long i=0;i<1000000000;i+=1000000) {
			int mil = 1000*60*60*24; //milliseconds in a day
			Date tempo = new Date(i); 
			System.out.println(new Date(i) +"  "+(int)tempo.getTime()/mil);
		}*/

		e.crossValidation("A2A",5);
	}
	
	public static void mainOLD(String[] args) {
		musichetta();
		Euclidean<PuntoConsolidato> tree;
		tree = new Euclidean<PuntoConsolidato>(2); //2 dimensioni
		
		System.out.println("Welcome to Indiana Jones");
		
		String query = "SELECT c.tempo, i.valore AS  vwapRatioFTSE, `minuto`, `vwapRatio`, `deltaVwap`, `ipercomprato60_85`, `gainLong99_99`, bookLiquidity, bookImbalance, volatility "+
			"from consolidata2minuti c, indicatori i "+  
			"where codalfa='A2A' AND i.indicatore='vwapRatioFTSE' AND i.tempo = from_unixtime(60*floor(unix_timestamp(c.tempo)/60)) " + 
			"order by c.tempo";
		
		TreeMap<Date, PuntoConsolidato> dati = new TreeMap<Date, PuntoConsolidato>();
		try {
			Class.forName("com.mysql.jdbc.Driver");
	        conn = DriverManager.getConnection("jdbc:mysql://localhost/?user=root&password=zxcvbnm");
	        //seleziono il database
	        conn.setCatalog("hedgefund");
	        
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			ArrayList<Float> t = new ArrayList<Float>();
			/*while(rs.next()) {
				double vwapRatio = rs.getFloat("vwapRatio");
				double vwapRatioFTSE = rs.getFloat("vwapRatioFTSE");
				Date tempo = rs.getDate("tempo");
				tree.addPoint(new double[]{vwapRatio,vwapRatioFTSE}, new PuntoConsolidato(vwapRatio,vwapRatioFTSE,rs.getFloat("gainLong99_99"),tempo));
			}
			rs = stmt.executeQuery(query);/**/
			while(rs.next()) {
				Date tempo = rs.getDate("tempo");
				//Date tempo = rs.getDate("tempo");
				Float vwapRatio = rs.getFloat("vwapRatio");
				Float vwapRatioFTSE = rs.getFloat("vwapRatioFTSE");
				//Float guadagno = rs.getFloat("gainLong99_99");
				Float guadagno = rs.getFloat("gainLong99_99");
				dati.put(rs.getTimestamp("tempo"), new PuntoConsolidato(vwapRatio, vwapRatioFTSE, guadagno, tempo));
			}
			for (Map.Entry<Date, PuntoConsolidato> e : dati.entrySet()) {
				PuntoConsolidato p = e.getValue();
				tree.addPoint(new double[]{p.vwapRatio,p.vwapRatioFTSE}, new PuntoConsolidato(p.vwapRatio,p.vwapRatioFTSE,p.guadagno,p.tempo));
			}/**/
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Imported "+tree.size()+" data points from MySQL");
		
		ArrayList<Integer> blacklistedDays = new ArrayList<Integer>();
		int mil = 1000*60*60*24; //milliseconds in a day
		try {
			System.setOut(new PrintStream(new FileOutputStream("output.csv")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		int K=5;
		double min=0.965,max=1.03,interval=0.0008;
		for (double y = min; y <= max; y+=interval) {
			System.out.print("\t"+y);
		}
		for (double x = min; x <= max; x+=interval) {
			System.out.print("\n"+x+"\t");
			for (double y = min; y <= max; y+=interval) {
				/*
				double i=interval*10;
				ArrayList<PuntoConsolidato> r = tree.rectSearch(new double[]{x-i,y-i}, new double[]{x+i,y+i});
				double sommaDist=0, sommaGuadagno=0;
				blacklistedDays.clear();
				int nViciniUsati=0;
				for (PuntoConsolidato p : r) {
					int pointDay = (int)p.tempo.getTime()/mil;
					//System.out.println("d="+p.distance);
					if (!blacklistedDays.contains(pointDay)) {
						//System.out.println(p.payload);
						blacklistedDays.add(pointDay);
						nViciniUsati++;
						sommaGuadagno += p.guadagno;
					}
				}/**/
				
				/*
				ArrayList<SearchResult<PuntoConsolidato>> r = tree.nearestNeighbours(new double[]{x,y}, 1000);
				double sommaDist=0, sommaGuadagno=0;
				blacklistedDays.clear();
				int nViciniUsati=0, nPositivi=0,nGiorniNelVicinato=0;
				float Tot=0;
				for (SearchResult<PuntoConsolidato> p : r) {
					int pointDay = (int)p.payload.tempo.getTime()/mil;
					//System.out.println("d="+p.distance);
					double maxD=0.00003;
					if (!blacklistedDays.contains(pointDay) && p.distance<maxD) {
					//if (p.distance<maxD) {
						//System.out.println(p.payload);
						if (!blacklistedDays.contains(pointDay)) {
							blacklistedDays.add(pointDay);
							nGiorniNelVicinato++;
						}
						nViciniUsati++;
						sommaGuadagno += p.payload.guadagno*(1-(p.distance/maxD));
						if (p.payload.guadagno>0) nPositivi++;
						Tot+=1-(p.distance/maxD);
					}
					//if (nViciniUsati>=K) break;
				}
				//System.out.println("n="+nViciniUsati);*/
				 
				//MAPPA SHARP
				ArrayList<SearchResult<PuntoConsolidato>> r = tree.nearestNeighbours(new double[]{x,y}, 1000);
				double sommaDist=0;
				blacklistedDays.clear();
				int nViciniUsati=0, nPositivi=0,nGiorniNelVicinato=0;
				float Tot=0;
				ArrayList<TradeResult> trades = new ArrayList<TradeResult>();
				for (SearchResult<PuntoConsolidato> p : r) {
					int pointDay = (int)p.payload.tempo.getTime()/mil;
					//System.out.println("d="+p.distance);
					double maxD=0.00003;
					if (!blacklistedDays.contains(pointDay) && p.distance<maxD) {
					//if (p.distance<maxD) {
						//System.out.println(p.payload);
						if (!blacklistedDays.contains(pointDay)) {
							blacklistedDays.add(pointDay);
							nGiorniNelVicinato++;
							//trades.add(new TradeResult((float)p.payload.guadagno));
						}
						trades.add(new TradeResult((float)p.payload.guadagno));
						nViciniUsati++;
						Tot+=1-(p.distance/maxD);
					}
					//if (nViciniUsati>=K) break;
				}
				
				if (nGiorniNelVicinato<2)
					System.out.print("0\t");
				else {
					Performance performanceVicinato = new Performance(trades);
					System.out.print(performanceVicinato.sharpRatio+"\t");
				}
					//System.out.print(Math.round(sommaGuadagno/Tot)+"\t");
			}
		}
		
		
		/*
		ArrayList<SearchResult<PuntoConsolidato>> r = tree.nearestNeighbours(new double[]{1,0}, 5);
		for (SearchResult<PuntoConsolidato> p : r) {
			System.out.println(p.distance);
			System.out.println(p.payload);
		}*/
		
	}
	
	

}
