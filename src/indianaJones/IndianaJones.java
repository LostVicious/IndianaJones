package indianaJones;
import indianaJones.KDTree.Euclidean;
import indianaJones.KDTree.SearchResult;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;



public class IndianaJones {
	
	static private Connection conn;
	DateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	public static void main(String[] args) {
		Euclidean<PuntoConsolidato> tree;
		tree = new Euclidean<PuntoConsolidato>(2); //2 dimensioni
		
		System.out.println("Welcome to Indiana Jones");
		
		String query = "SELECT c.tempo, i.valore AS  vwapRatioFTSE, `minuto`, `vwapRatio`, `deltaVwap`, `ipercomprato60_85`, `gainLong99_99`, bookLiquidity, bookImbalance, volatility "+
			"from consolidata2minuti c, indicatori i "+  
			"where codalfa='AZM' AND i.indicatore='vwapRatioFTSE' AND i.tempo = from_unixtime(60*floor(unix_timestamp(c.tempo)/60)) " + 
			"order by c.tempo";
		
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
				tree.addPoint(new double[]{vwapRatio,vwapRatioFTSE}, new PuntoConsolidato(vwapRatio,vwapRatioFTSE,rs.getFloat("gainLong99_99"),tempo));
			}
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
				}*/
				
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
						sommaDist += p.distance*100000;
						sommaGuadagno += p.payload.guadagno*(1-(p.distance/maxD));
						if (p.payload.guadagno>0) nPositivi++;
						Tot+=1-(p.distance/maxD);
					}
					//if (nViciniUsati>=K) break;
				}
				//System.out.println("n="+nViciniUsati);
				 
				if (nGiorniNelVicinato<3)
					System.out.print("0\t");
				else
					System.out.print((float)nPositivi/nViciniUsati+"\t");
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
	
	String dataQuery(String codalfa, String timeCondition) {
		String query = "SELECT c.tempo, i.valore AS  vwapRatioFTSE, `minuto`, `vwapRatio`, `deltaVwap`, `ipercomprato60_85`, `gainLong99_99`, bookLiquidity, bookImbalance, volatility "+
				"from consolidata2minuti c, indicatori i "+  
				"where codalfa='"+codalfa+"' AND "+timeCondition+" AND i.indicatore='vwapRatioFTSE' AND i.tempo = from_unixtime(60*floor(unix_timestamp(c.tempo)/60)) " + 
				"order by c.tempo";
		return query;
	}
	
	void crossValidation(String codalfa, int N) {
		Date min = new Date(0),max = new Date(0);
		long deltaT = Long.MAX_VALUE;
		String query = "SELECT MIN(tempo) as mi, MAX(tempo) AS ma FROM consolidata2minuti WHERE codalfa='"+codalfa+"'";
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()) {
				min = rs.getDate("mi");
				max = rs.getDate("ma");
			}
			deltaT = max.getTime()-min.getTime()/N;
		
			//qui abbiamo min max e deltaT
			
			
			for (long startT=min.getTime();startT<max.getTime();startT+=deltaT) {
				//genera tree prendendo dati da mi a startT e da startT+deltaT a ma
				//carico dati Training:
				query=dataQuery(codalfa, "(c.tempo>='"+tsFormat.format(min)+"' AND c.tempo<'"+tsFormat.format(new Date(startT))+"') "+
										"OR (c.tempo>='"+tsFormat.format(new Date(startT+deltaT))+"' AND c.tempo<='"+tsFormat.format(max)+"')");
				
				//carico i dati nella Mappa
				Euclidean<PuntoConsolidato> tree = new Euclidean<PuntoConsolidato>(2); //2 dimensioni
				rs = stmt.executeQuery(query);
				ArrayList<Float> t = new ArrayList<Float>();
				while(rs.next()) {
					double vwapRatio = rs.getFloat("vwapRatio");
					double vwapRatioFTSE = rs.getFloat("vwapRatioFTSE");
					Date tempo = rs.getDate("tempo");
					tree.addPoint(new double[]{vwapRatio,vwapRatioFTSE}, new PuntoConsolidato(vwapRatio,vwapRatioFTSE,rs.getFloat("gainLong99_99"),tempo));
				}
			}
			
				//testa il tree con dati da startT a starT+deltaT:
					//blacklist dei giorni, trada sulla prima occasione della giornata
			
					//output i singoli trade che fa, calcola guadagnoMedio
			
				//output separatore
				//salva guadagnoMedio in array
			
			//outputta i guadagni medi delle varie crossvalidation, e il guadagno medio totale
			
			//return guadagno medio totale
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void testaTitoli() {
		//per titolo in titoli 
		//output crossvalidation(titolo)
	}
	

}
