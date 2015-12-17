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
import java.nio.MappedByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.UnsupportedAudioFileException;



public class IndianaJones {
	
	static private Connection conn;
	static DateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	public static int mil = 1000*60*60*24; //milliseconds in a day
	
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
		musichetta();
		Evaluation e = new Evaluation();
		String[] titoliDaTestare = {"A2A","ATL","AZM","AGL","BP","BPE","BMPS","CPR","EGPW","ENEL","ENI","EXO","FCA","FNC","G","ISP","LUX","MB","MED","MONC","MS","PMI","PRY","SFER","SPM","SRG","STM","STS","TEN","TIT","TOD","TRN","UBI","UCG","US","YNAP"};
		//Euclidean<PuntoConsolidato> tree = e.generateTreeAll("c.tempo<"+tsFormat.format(new Date()));
		
		Euclidean<PuntoConsolidato> tree = e.generateTreeAll("c.tempo<'2015-08-31 22:04:30'");
		printTree(tree, "", new MetodoPercentuale());
		//Euclidean<PuntoConsolidato> tree = e.generateTreeAll("1=1");
		ArrayList<TradeResult> trades = new ArrayList<TradeResult>();
		for (String cod : titoliDaTestare) {
			ArrayList<PuntoConsolidato> testSet = e.puntiTitoloPeriodo(cod, "c.tempo>'2015-08-31 22:04:30'");
			//ArrayList<PuntoConsolidato> testSet = e.puntiTitoloPeriodo(cod, "1=1");
			Performance p = e.testSampleGroup(testSet, tree,new MetodoPercentuale());
			System.out.println(cod+"\t"+p);
			trades.addAll(p.trades);
		}
		System.out.println(new Performance(trades));
		/*for (long i=0;i<1000000000;i+=1000000) {
			int mil = 1000*60*60*24; //milliseconds in a day
			Date tempo = new Date(i); 
			System.out.println(new Date(i) +"  "+(int)tempo.getTime()/mil);
		}*/
	}
	
	public static void printTree(Euclidean<PuntoConsolidato> tree,String filename,Metodo metodo) {
		double min=0.950,max=1.06,interval=0.0008;
		
		/*try {
			System.setOut(new PrintStream(new FileOutputStream("output.csv")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}*/
		
		for (double y = min; y <= max; y+=interval) {
			System.out.print("\t"+y);
		}
		for (double x = min; x <= max; x+=interval) {
			System.out.print("\n"+x+"\t");
			for (double y = min; y <= max; y+=interval) {
				metodo.call(tree, x, y);
				System.out.print(metodo.getLastResult()+"\t");
			}
		}
	}
	
	
	public static void mainOLD(String[] args) {
		musichetta();
		Euclidean<PuntoConsolidato> tree;
		tree = new Euclidean<PuntoConsolidato>(2); //2 dimensioni
		
		System.out.println("Welcome to Indiana Jones");
		
		String query = "SELECT codalfa, c.tempo, i.valore AS  vwapRatioFTSE, `minuto`, `vwapRatio`, `deltaVwap`, `ipercomprato60_85`, `gainLong99_99`, bookLiquidity, bookImbalance, volatility "+
			"from consolidata2minuti c, indicatori i "+  
			//"where codalfa='A2A' AND i.indicatore='vwapRatioFTSE' AND i.tempo = from_unixtime(60*floor(unix_timestamp(c.tempo)/60)) " +
			"where i.indicatore='vwapRatioFTSE' AND i.tempo = from_unixtime(60*floor(unix_timestamp(c.tempo)/60)) " +
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
			while(rs.next()) {
				double vwapRatio = rs.getFloat("vwapRatio");
				double vwapRatioFTSE = rs.getFloat("vwapRatioFTSE");
				Date tempo = rs.getDate("tempo");
				tree.addPoint(new double[]{vwapRatio,vwapRatioFTSE}, new PuntoConsolidato(rs.getString("codalfa"),vwapRatio,vwapRatioFTSE,rs.getFloat("gainLong99_99"),tempo));
			}
			/*rs = stmt.executeQuery(query);/*
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
		/*try {
			System.setOut(new PrintStream(new FileOutputStream("output.csv")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}*/
		int K=5;
		//double min=0.965,max=1.03,interval=0.0008;
		double min=0.950,max=1.06,interval=0.0008;
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
					int pointDay = (int)(p.payload.tempo.getTime()/mil);
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
					int pointDay = (int)(p.payload.tempo.getTime()/mil);
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
				
				mappaSharpe(tree, x, y);
				//mappaPercentuale(tree, x, y);
				
				 /*
				//MAPPA SHARP
				ArrayList<SearchResult<PuntoConsolidato>> r = tree.nearestNeighbours(new double[]{x,y}, 1000);
				double sommaDist=0;
				blacklistedDays.clear();
				int nViciniUsati=0, nPositivi=0,nGiorniNelVicinato=0;
				float Tot=0;
				ArrayList<TradeResult> trades = new ArrayList<TradeResult>();
				for (SearchResult<PuntoConsolidato> p : r) {
					int pointDay = (int)(p.payload.tempo.getTime()/mil);
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
				}*/
					//System.out.print(Math.round(sommaGuadagno/Tot)+"\t");
			}
		}
		
	}
	
	static void mappaPercentuale(Euclidean<PuntoConsolidato> tree,double x,double y) {
		ArrayList<SearchResult<PuntoConsolidato>> r = tree.nearestNeighbours(new double[]{x,y}, 1000);
		ArrayList<Integer> blacklistedDays = new ArrayList<Integer>();
		
		int nViciniUsati=0, nPositivi=0,nGiorniNelVicinato=0;
		for (SearchResult<PuntoConsolidato> p : r) {
			int pointDay = (int)(p.payload.tempo.getTime()/mil);
			double maxD=0.00010;
			//double maxD=0.00003;
			//if (!blacklistedDays.contains(pointDay) && p.distance<maxD) {
			if (p.distance<maxD) {
				//System.out.println(p.payload);
				if (!blacklistedDays.contains(pointDay)) {
					blacklistedDays.add(pointDay);
					nGiorniNelVicinato++;
				}
				nViciniUsati++;
				if (p.payload.guadagno>0) nPositivi++;
			}
		}
		if (nGiorniNelVicinato<2)
			System.out.print("0\t");
		else {
			System.out.print((float)nPositivi/nViciniUsati+"\t");
		}
	}
	
	static void mappaSharpe(Euclidean<PuntoConsolidato> tree,double x,double y) {
		ArrayList<SearchResult<PuntoConsolidato>> r = tree.nearestNeighbours(new double[]{x,y}, 1000);
		ArrayList<Integer> blacklistedDays = new ArrayList<Integer>();
		
		//mappa: "AZM20150101" -> orario ingresso
		LinkedHashMap<String, Date> blackList = new LinkedHashMap<String, Date>();
		LinkedHashMap<String, TradeResult> trades = new LinkedHashMap<String, TradeResult>();
		int nGiorniNelVicinato=0;
		for (SearchResult<PuntoConsolidato> p : r) {
			int pointDay = (int)(p.payload.tempo.getTime()/mil);
			//System.out.println("d="+p.distance);
			double maxD=0.00004; //0.00003
			//if (!blacklistedDays.contains(pointDay) && p.distance<maxD) {
			if (p.distance<maxD) {
				//System.out.println(p.payload);
				if (!blacklistedDays.contains(pointDay)) {
					blacklistedDays.add(pointDay);
					nGiorniNelVicinato++;
					//trades.add(new TradeResult((float)p.payload.guadagno));
				}
				String key = p.payload.codalfa+pointDay;
				//System.out.println(pointDay);
				//System.out.println(key);
				//usa il dato solo se e' l'unico per quel titolo per quel giorno o se e'
				//antecedente al trade che abbiamo per quel titolo per quel giorno
				trades.put(key, new TradeResult((float)p.payload.guadagno));
				/*if (!blackList.containsKey(key)) {
					trades.put(key, new TradeResult((float)p.payload.guadagno));
				} else if (p.payload.tempo.getTime() < blackList.get(key).getTime()) {
					trades.put(key, new TradeResult((float)p.payload.guadagno));
				}*/
			}
			//if (nViciniUsati>=K) break;
		}
		
		if (nGiorniNelVicinato<2)
			System.out.print("0\t");
		else {
			Performance performanceVicinato = new Performance(new ArrayList<TradeResult>(trades.values()));
			System.out.print(performanceVicinato.sharpRatio+"\t");
		}
	}

}
