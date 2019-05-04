package trk;

import java.io.File;
//import org.junit.Test;
import java.util.ArrayList;
import javax.swing.JFrame;
//import static org.junit.Assert.*;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.math.F1D;

import org.jlab.clas.physics.Particle;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.groot.group.DataGroup;
import org.jlab.utils.groups.IndexedList;

/**
 *
 * Analyze tracking results
 *
 * TODO:  
 * 
 * @author devita
 */
public class TrackingTest {

    static final boolean debug=false;
    
    String  analysisName = "TrackingTest";
    
    IndexedList<DataGroup> dataGroups      = new IndexedList<DataGroup>(1);
    EmbeddedCanvasTabbed   canvasTabbed    = null;
    ArrayList<String>      canvasTabNames  = new ArrayList<String>();
    
    // these correspond to Joseph's two-particle event generater:
    static final int electronSector=1;
    static final int hadronSector=3;

    boolean isForwardTagger=false;
    boolean isCentral=false;

    int fdCharge = 0;

    int nNegTrackEvents = 0;
    int nTwoTrackEvents = 0;
    
    int nEvents = 0;
    
    double ebeam = 10.6;

//    @Test
    public static void main(String arg[]){
        
//        System.setProperty("java.awt.headless", "true"); // this should disable the Xwindow requirement
        GStyle.getAxisAttributesX().setTitleFontSize(24);
        GStyle.getAxisAttributesX().setLabelFontSize(18);
        GStyle.getAxisAttributesY().setTitleFontSize(24);
        GStyle.getAxisAttributesY().setLabelFontSize(18);
        GStyle.getAxisAttributesZ().setLabelFontSize(14);
//        GStyle.setPalette("kDefault");
        GStyle.getAxisAttributesX().setLabelFontName("Avenir");
        GStyle.getAxisAttributesY().setLabelFontName("Avenir");
        GStyle.getAxisAttributesZ().setLabelFontName("Avenir");
        GStyle.getAxisAttributesX().setTitleFontName("Avenir");
        GStyle.getAxisAttributesY().setTitleFontName("Avenir");
        GStyle.getAxisAttributesZ().setTitleFontName("Avenir");
        GStyle.setGraphicsFrameLineWidth(1);
        GStyle.getH1FAttributes().setLineWidth(1);
        GStyle.getH1FAttributes().setOptStat("1111");
        
        TrackingTest ttest = new TrackingTest();
        
        ttest.setAnalysisTabNames("TBT Resolution","TBT negative part. resolution", "TBT Vertex","TBT Positive Tracks","TBT Negative Tracks",
                                  "TBT Positive Tracks by Sector","TBT Negative Tracks by Sector","TBT-CVT Vertex Comparison",
                                  "CVT Resolution","CVT Positive Tracks","CVT Negative Tracks","CVT Vertex");

        ttest.createHistos();
        
        String resultDir=System.getProperty("RESULTS");
        File dir = new File(resultDir);
        if (!dir.isDirectory()) {
            System.err.println("Cannot find output directory");
//            assertEquals(false, true);
        }
        String inname = System.getProperty("INPUTFILE");
        String fileName=resultDir + "/out_" + inname + ".hipo";
        File file = new File(fileName);
        if (!file.exists() || file.isDirectory()) {
            System.err.println("Cannot find input file.");
//            assertEquals(false, true); // use method from org.junit.Assert.* library to be able to do quantitative checks and test specific conditions
        }

        HipoDataSource reader = new HipoDataSource();
        reader.open(fileName);

        while (reader.hasEvent()) {
	    DataEvent event = reader.getNextEvent();
            ttest.processEvent(event);
        }
        reader.close();

        JFrame frame = new JFrame("Tracking");
        frame.setSize(1200, 800);
        frame.add(ttest.canvasTabbed);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        ttest.analyze();
        ttest.plotHistos();
       
        ttest.printCanvas(resultDir,inname);

    }


    private void processEvent(DataEvent event) {
	nEvents++;
        if((nEvents%10000) == 0) System.out.println("Analyzed " + nEvents + " events");
        int run = 0;
        if(event.hasBank("RUN::config")) {
            run = event.getBank("RUN::config").getInt("run", 0);            
        }
        else {
            return;
        }
        if(run>2365 && run<=2597)      ebeam=2.217;
        else if(run>3028 && run<=3105) ebeam=6.424;
        else if(run>3105 && run<=3817) ebeam=10.594;
        else if(run>3817 && run<=3861) ebeam=6.424;
        else if(run>3861)              ebeam=10.594;
        // process event info and save into data group
        Particle partGenNeg = null;
        Particle partGenPos = null;
        Particle partRecNeg = null;
        Particle partRecPos = null;
        Particle partRecCDNeg = null;
        Particle partRecCDPos = null;

        // forward tracking plots
        if(event.hasBank("TimeBasedTrkg::TBTracks")==true){
            DataBank  bank = event.getBank("TimeBasedTrkg::TBTracks");
            int rows = bank.rows();
            for(int loop = 0; loop < rows; loop++){
                int pidCode=0;
                if(bank.getByte("q", loop)==-1) pidCode = 11;
                else if(bank.getByte("q", loop)==1) pidCode = 211;
                else pidCode = 22;
                Particle recParticle = new Particle(
                                          pidCode,
                                          bank.getFloat("p0_x", loop),
                                          bank.getFloat("p0_y", loop),
                                          bank.getFloat("p0_z", loop),
                                          bank.getFloat("Vtx0_x", loop),
                                          bank.getFloat("Vtx0_y", loop),
                                          bank.getFloat("Vtx0_z", loop));
                if(bank.getShort("ndf", loop)>0) recParticle.setProperty("chi2", bank.getFloat("chi2", loop)/bank.getShort("ndf", loop));
                if(recParticle.charge()>0) {
                    dataGroups.getItem(2).getH1F("hi_p_pos").fill(recParticle.p());
                    dataGroups.getItem(2).getH1F("hi_theta_pos").fill(Math.toDegrees(recParticle.theta()));
                    dataGroups.getItem(2).getH1F("hi_phi_pos").fill(Math.toDegrees(recParticle.phi()));
                    dataGroups.getItem(2).getH1F("hi_chi2_pos").fill(recParticle.getProperty("chi2"));
                    dataGroups.getItem(2).getH1F("hi_vz_pos").fill(recParticle.vz());
                    dataGroups.getItem(4).getH2F("hi_vz_vs_theta_pos").fill(Math.toDegrees(recParticle.theta()),recParticle.vz());
                    dataGroups.getItem(4).getH2F("hi_vxy_pos").fill(recParticle.vx(),recParticle.vy());
                    dataGroups.getItem(4).getH2F("hi_vz_vs_phi_pos").fill(recParticle.vz(),Math.toDegrees(recParticle.phi()));
                   if(recParticle.p()>2.&& Math.toDegrees(recParticle.theta())>10.) {
                        dataGroups.getItem(2).getH1F("hi_vz_pos_cut").fill(recParticle.vz());
                    }
                    dataGroups.getItem(2).getH2F("hi_theta_p_pos").fill(recParticle.p(),Math.toDegrees(recParticle.theta()));
                    dataGroups.getItem(2).getH2F("hi_theta_phi_pos").fill(Math.toDegrees(recParticle.phi()),Math.toDegrees(recParticle.theta()));
                    dataGroups.getItem(2).getH2F("hi_chi2_vz_pos").fill(recParticle.vz(),recParticle.getProperty("chi2"));
                }
                else {
                    dataGroups.getItem(1).getH1F("hi_p_neg").fill(recParticle.p());
                    dataGroups.getItem(1).getH1F("hi_theta_neg").fill(Math.toDegrees(recParticle.theta()));
                    dataGroups.getItem(1).getH1F("hi_phi_neg").fill(Math.toDegrees(recParticle.phi()));
                    dataGroups.getItem(1).getH1F("hi_chi2_neg").fill(recParticle.getProperty("chi2"));
                    dataGroups.getItem(1).getH1F("hi_vz_neg").fill(recParticle.vz());
                    dataGroups.getItem(4).getH2F("hi_vz_vs_theta_neg").fill(Math.toDegrees(recParticle.theta()),recParticle.vz());
                    dataGroups.getItem(4).getH2F("hi_vxy_neg").fill(recParticle.vx(),recParticle.vy());
                    dataGroups.getItem(4).getH2F("hi_vz_vs_phi_neg").fill(recParticle.vz(),Math.toDegrees(recParticle.phi()));
                    if(recParticle.p()>2.&& Math.toDegrees(recParticle.theta())>15.) {
                        dataGroups.getItem(1).getH1F("hi_vz_neg_cut").fill(recParticle.vz());
                    }
                    dataGroups.getItem(1).getH2F("hi_theta_p_neg").fill(recParticle.p(),Math.toDegrees(recParticle.theta()));
                    dataGroups.getItem(1).getH2F("hi_theta_phi_neg").fill(Math.toDegrees(recParticle.phi()),Math.toDegrees(recParticle.theta()));                    
                    dataGroups.getItem(1).getH2F("hi_chi2_vz_neg").fill(recParticle.vz(),recParticle.getProperty("chi2"));;
                }

                int sector = bank.getByte("sector", loop);
                if(recParticle.charge()>0) {
                    dataGroups.getItem(9).getH1F("hi_vz_pos_sec_" + sector).fill(recParticle.vz());
                    if(recParticle.p()>2.&& Math.toDegrees(recParticle.theta())>10.) {
                        dataGroups.getItem(9).getH1F("hi_vz_pos_sec_" + sector + "_cut").fill(recParticle.vz());
                    }
                }
                else {
                    dataGroups.getItem(10).getH1F("hi_vz_neg_sec_" + sector).fill(recParticle.vz());
                    if(recParticle.p()>2.&& Math.toDegrees(recParticle.theta())>15.) {
                        dataGroups.getItem(10).getH1F("hi_vz_neg_sec_" + sector + "_cut").fill(recParticle.vz());
                    }
                }

	        if(partRecNeg==null && recParticle.charge()<0) {
		    partRecNeg = new Particle();
		    partRecNeg.copy(recParticle);
		}
                if(partRecPos==null && recParticle.charge()>0) {
		    partRecPos = new Particle();
		    partRecPos.copy(recParticle);
		}
            }
        }

        // central tracking plots
        if(event.hasBank("CVTRec::Tracks")==true){
            DataBank  bank = event.getBank("CVTRec::Tracks");
            int rows = bank.rows();
            for(int loop = 0; loop < rows; loop++){
                int pidCode=0;
                if(bank.getByte("q", loop)==-1) pidCode = -211;
                else if(bank.getByte("q", loop)==1) pidCode = 211; 
                else pidCode = 22;; 
                Particle recParticle = new Particle(
                                          pidCode,
                                          bank.getFloat("pt", loop)*Math.cos(bank.getFloat("phi0", loop)),
                                          bank.getFloat("pt", loop)*Math.sin(bank.getFloat("phi0", loop)),
                                          bank.getFloat("pt", loop)*bank.getFloat("tandip", loop),
                                          -bank.getFloat("d0", loop)*Math.sin(bank.getFloat("phi0", loop)),
                                          bank.getFloat("d0", loop)*Math.cos(bank.getFloat("phi0", loop)),
                                          bank.getFloat("z0", loop));
                if(bank.getShort("ndf", loop)>0) recParticle.setProperty("chi2", bank.getFloat("chi2", loop)/bank.getShort("ndf", loop));
		if(recParticle.charge()>0) {
                    dataGroups.getItem(6).getH1F("hi_p_pos").fill(recParticle.p());
                    dataGroups.getItem(6).getH1F("hi_theta_pos").fill((float) Math.toDegrees(recParticle.theta()));
                    dataGroups.getItem(6).getH1F("hi_phi_pos").fill(Math.toDegrees(recParticle.phi()));
                    dataGroups.getItem(6).getH1F("hi_vz_pos").fill(recParticle.vz());
                    dataGroups.getItem(6).getH2F("hi_theta_p_pos").fill(recParticle.p(),Math.toDegrees(recParticle.theta()));
                    dataGroups.getItem(6).getH2F("hi_theta_phi_pos").fill(Math.toDegrees(recParticle.phi()),Math.toDegrees(recParticle.theta()));
                    dataGroups.getItem(6).getH1F("hi_chi2_pos").fill(recParticle.getProperty("chi2"));
                    dataGroups.getItem(6).getH2F("hi_chi2_vz_pos").fill(recParticle.vz(),recParticle.getProperty("chi2"));
                    dataGroups.getItem(8).getH2F("hi_vz_vs_theta_pos").fill(Math.toDegrees(recParticle.theta()),recParticle.vz());
                    dataGroups.getItem(8).getH2F("hi_vxy_pos").fill(recParticle.vx(),recParticle.vy());
                    dataGroups.getItem(8).getH2F("hi_vz_vs_phi_pos").fill(recParticle.vz(),Math.toDegrees(recParticle.phi()));
                }
                else {
                    dataGroups.getItem(5).getH1F("hi_p_neg").fill(recParticle.p());
                    dataGroups.getItem(5).getH1F("hi_theta_neg").fill(Math.toDegrees(recParticle.theta()));
                    dataGroups.getItem(5).getH1F("hi_phi_neg").fill(Math.toDegrees(recParticle.phi()));
                    dataGroups.getItem(5).getH1F("hi_vz_neg").fill(recParticle.vz());
                    dataGroups.getItem(5).getH2F("hi_theta_p_neg").fill(recParticle.p(),Math.toDegrees(recParticle.theta()));
                    dataGroups.getItem(5).getH2F("hi_theta_phi_neg").fill(Math.toDegrees(recParticle.phi()),Math.toDegrees(recParticle.theta()));
                    dataGroups.getItem(5).getH1F("hi_chi2_neg").fill(recParticle.getProperty("chi2"));
                    dataGroups.getItem(5).getH2F("hi_chi2_vz_neg").fill(recParticle.vz(),recParticle.getProperty("chi2"));
                    dataGroups.getItem(8).getH2F("hi_vz_vs_theta_neg").fill(Math.toDegrees(recParticle.theta()),recParticle.vz());
                    dataGroups.getItem(8).getH2F("hi_vxy_neg").fill(recParticle.vx(),recParticle.vy());                    
                    dataGroups.getItem(8).getH2F("hi_vz_vs_phi_neg").fill(recParticle.vz(),Math.toDegrees(recParticle.phi()));
                }
                if(partRecNeg==null && recParticle.charge()<0) {
                    partRecCDNeg = new Particle();
                    partRecCDNeg.copy(recParticle);
                }
                if(partRecPos==null && recParticle.charge()>0) {
                    partRecCDPos = new Particle();
                    partRecCDPos.copy(recParticle);
                }
            }
        }

        if(event.hasBank("MC::Particle")==true){
            DataBank genBank = event.getBank("MC::Particle");
            int nrows = genBank.rows();
            for(int loop = 0; loop < nrows; loop++) {   
                Particle genPart = new Particle(
                                              genBank.getInt("pid", loop),
                                              genBank.getFloat("px", loop),
                                              genBank.getFloat("py", loop),
                                              genBank.getFloat("pz", loop),
                                              genBank.getFloat("vx", loop),
                                              genBank.getFloat("vy", loop),
                                              genBank.getFloat("vz", loop));
                if(genPart.charge()==-1  && partGenNeg==null && genPart.theta()!=0) partGenNeg=genPart;
                if(genPart.charge()==1   && partGenPos==null) partGenPos=genPart;
                if(partRecCDNeg != null) {
                        if(testMCpart(genPart,partRecCDNeg)) {
                            dataGroups.getItem(7).getH1F("hi_dp_neg").fill((partRecCDNeg.p()-genPart.p())/genPart.p());
                            dataGroups.getItem(7).getH1F("hi_dtheta_neg").fill(Math.toDegrees(partRecCDNeg.theta()-genPart.theta()));
                            dataGroups.getItem(7).getH1F("hi_dphi_neg").fill(Math.toDegrees(partRecCDNeg.phi()-genPart.phi()));
                            dataGroups.getItem(7).getH1F("hi_dvz_neg").fill(partRecCDNeg.vz()-genPart.vz());
                        }
                }
                if(partRecCDPos != null) {
                        if(testMCpart(genPart,partRecCDPos)) {
                            dataGroups.getItem(7).getH1F("hi_dp_pos").fill((partRecCDPos.p()-genPart.p())/genPart.p());
                            dataGroups.getItem(7).getH1F("hi_dtheta_pos").fill(Math.toDegrees(partRecCDPos.theta()-genPart.theta()));
                            dataGroups.getItem(7).getH1F("hi_dphi_pos").fill(Math.toDegrees(partRecCDPos.phi()-genPart.phi()));
                            dataGroups.getItem(7).getH1F("hi_dvz_pos").fill(partRecCDPos.vz()-genPart.vz());
                        }
                }
            }
            if(partGenNeg != null && partRecNeg != null) {
//                   if(testMCpart(genPart,partRecNeg)) {
                        dataGroups.getItem(3).getH1F("hi_dp_neg").fill((partRecNeg.p()-partGenNeg.p())/partGenNeg.p());
                        dataGroups.getItem(3).getH2F("hi_dp_p_neg").fill(partGenNeg.p(),(partRecNeg.p()-partGenNeg.p())/partGenNeg.p());
                        dataGroups.getItem(3).getH2F("hi_dp_theta_neg").fill(Math.toDegrees(partGenNeg.theta()),(partRecNeg.p()-partGenNeg.p())/partGenNeg.p());
                        dataGroups.getItem(3).getH2F("hi_dp_phi_neg").fill(Math.toDegrees(partGenNeg.phi()),(partRecNeg.p()-partGenNeg.p())/partGenNeg.p());
                        dataGroups.getItem(3).getH1F("hi_dtheta_neg").fill(Math.toDegrees(partRecNeg.theta()-partGenNeg.theta()));
                        dataGroups.getItem(3).getH1F("hi_dphi_neg").fill(Math.toDegrees(partRecNeg.phi()-partGenNeg.phi()));
                        dataGroups.getItem(3).getH1F("hi_dvz_neg").fill(partRecNeg.vz()-partGenNeg.vz());
//                    }
            }
            if(partGenPos != null && partRecPos != null) {
//                    if(testMCpart(genPart,partRecPos)) {
                        dataGroups.getItem(3).getH1F("hi_dp_pos").fill((partRecPos.p()-partGenPos.p())/partGenPos.p());
                        dataGroups.getItem(3).getH1F("hi_dtheta_pos").fill(Math.toDegrees(partRecPos.theta()-partGenPos.theta()));
                        dataGroups.getItem(3).getH1F("hi_dphi_pos").fill(Math.toDegrees(partRecPos.phi()-partGenPos.phi()));
                        dataGroups.getItem(3).getH1F("hi_dvz_pos").fill(partRecPos.vz()-partGenPos.vz());
//                    }
	    }
        } 

        
        // central vs. forward
        if(event.hasBank("CVTRec::Tracks")==true && event.hasBank("TimeBasedTrkg::TBTracks")==true){
            DataBank  CVTbank = event.getBank("CVTRec::Tracks");
            DataBank  TBTbank = event.getBank("TimeBasedTrkg::TBTracks");
            int rowsCVT = CVTbank.rows();
	    DataBank  bank = event.getBank("CVTRec::Tracks");
	    for(int loop = 0; loop < rowsCVT; loop++){
                int pidCode=0;
                if(CVTbank.getByte("q", loop)==-1) pidCode = -211;
                else if(CVTbank.getByte("q", loop)==1) pidCode = 211; 
                else pidCode = 22; 
                Particle recParticleCVT = new Particle(
                                          pidCode,
                                          CVTbank.getFloat("pt", loop)*Math.cos(CVTbank.getFloat("phi0", loop)),
                                          CVTbank.getFloat("pt", loop)*Math.sin(CVTbank.getFloat("phi0", loop)),
                                          CVTbank.getFloat("pt", loop)*CVTbank.getFloat("tandip", loop),
                                          -CVTbank.getFloat("d0", loop)*Math.sin(CVTbank.getFloat("phi0", loop)),
                                          CVTbank.getFloat("d0", loop)*Math.cos(CVTbank.getFloat("phi0", loop)),
                                          CVTbank.getFloat("z0", loop));
                if(recParticleCVT.charge()!=0) {  
                    int rowsTBT = TBTbank.rows();
                    for(int loopTBT = 0; loopTBT < rowsTBT; loopTBT++){
                        int pidCodeTBT=0;
                        if(TBTbank.getByte("q", loopTBT)==-1) pidCodeTBT = 11;
                        else if(TBTbank.getByte("q", loopTBT)==1) pidCodeTBT = 211;
                        else pidCodeTBT = 22;
                        Particle recParticleTBT = new Particle(
                                                pidCodeTBT,
                                                TBTbank.getFloat("p0_x", loopTBT),
                                                TBTbank.getFloat("p0_y", loopTBT),
                                                TBTbank.getFloat("p0_z", loopTBT),
                                                TBTbank.getFloat("Vtx0_x", loopTBT),
                                                TBTbank.getFloat("Vtx0_y", loopTBT),
                                                TBTbank.getFloat("Vtx0_z", loopTBT));
                        if(recParticleTBT.charge()>0) {
                            dataGroups.getItem(11).getH2F("vertex_CVT_TBT_pos").fill(recParticleTBT.vz(), recParticleCVT.vz());
                            dataGroups.getItem(11).getH1F("vertex_CVT_TBT_diff_pos").fill(recParticleTBT.vz()-recParticleCVT.vz());
                        }
                        else if(recParticleTBT.charge()<0) {
                            dataGroups.getItem(11).getH2F("vertex_CVT_TBT_neg").fill(recParticleTBT.vz(), recParticleCVT.vz());
                            dataGroups.getItem(11).getH1F("vertex_CVT_TBT_diff_neg").fill(recParticleTBT.vz()-recParticleCVT.vz());
                        }
                     }
                }
            }
        } 
    }

    
    private void createHistos() {
    
            // negative tracks
        H1F hi_p_neg = new H1F("hi_p_neg", "hi_p_neg", 100, 0.0, 8.0);     
        hi_p_neg.setTitleX("p (GeV)");
        hi_p_neg.setTitleY("Counts");
        H1F hi_theta_neg = new H1F("hi_theta_neg", "hi_theta_neg", 100, 0.0, 40.0); 
        hi_theta_neg.setTitleX("#theta (deg)");
        hi_theta_neg.setTitleY("Counts");
        H1F hi_phi_neg = new H1F("hi_phi_neg", "hi_phi_neg", 100, -180.0, 180.0);   
        hi_phi_neg.setTitleX("#phi (deg)");
        hi_phi_neg.setTitleY("Counts");
        H1F hi_chi2_neg = new H1F("hi_chi2_neg", "hi_chi2_neg", 100, 0.0, 180.0);   
        hi_chi2_neg.setTitleX("#chi2");
        hi_chi2_neg.setTitleY("Counts");
        H1F hi_vz_neg = new H1F("hi_vz_neg", "hi_vz_neg", 100, -15.0, 15.0);   
        hi_vz_neg.setTitleX("Vz (cm)");
        hi_vz_neg.setTitleY("Counts");
        H1F hi_vz_neg_cut = new H1F("hi_vz_neg_cut", "hi_vz_neg_cut", 100, -15.0, 15.0);   
        hi_vz_neg_cut.setTitleX("Vz (cm)");
        hi_vz_neg_cut.setTitleY("Counts");
        hi_vz_neg_cut.setLineColor(2);
        F1D f1_vz_neg = new F1D("f1_vz_neg","[amp]*gaus(x,[mean],[sigma])", -5.0, 5.0);
        f1_vz_neg.setParameter(0, 0);
        f1_vz_neg.setParameter(1, 0);
        f1_vz_neg.setParameter(2, 1.0);
        f1_vz_neg.setLineWidth(2);
        f1_vz_neg.setLineColor(1);
        f1_vz_neg.setOptStat("1111");
        H2F hi_theta_p_neg = new H2F("hi_theta_p_neg", "hi_theta_p_neg", 100, 0.0, 8.0, 100, 0.0, 40.0); 
        hi_theta_p_neg.setTitleX("p (GeV)");
        hi_theta_p_neg.setTitleY("#theta (deg)");        
        H2F hi_theta_phi_neg = new H2F("hi_theta_phi_neg", "hi_theta_phi_neg", 100, -180.0, 180.0, 100, 0.0, 40.0); 
        hi_theta_phi_neg.setTitleX("#phi (deg)");
        hi_theta_phi_neg.setTitleY("#theta (deg)");        
        H2F hi_chi2_vz_neg = new H2F("hi_chi2_vz_neg", "hi_chi2_vz_neg", 100, -15.0, 15.0, 100, 0.0, 180.0);   
        hi_chi2_vz_neg.setTitleX("Vz (cm)");
        hi_chi2_vz_neg.setTitleY("#chi2");
        DataGroup dg_neg = new DataGroup(4,2);
        dg_neg.addDataSet(hi_p_neg, 0);
        dg_neg.addDataSet(hi_theta_neg, 1);
        dg_neg.addDataSet(hi_phi_neg, 2);
        dg_neg.addDataSet(hi_chi2_neg, 3);
        dg_neg.addDataSet(hi_vz_neg, 4);
        dg_neg.addDataSet(hi_vz_neg_cut, 4);
        dg_neg.addDataSet(f1_vz_neg, 4);
        dg_neg.addDataSet(hi_theta_p_neg, 5);
        dg_neg.addDataSet(hi_theta_phi_neg, 6);
        dg_neg.addDataSet(hi_chi2_vz_neg, 7);
        dataGroups.add(dg_neg, 1);
        // positive trakcs
        H1F hi_p_pos = new H1F("hi_p_pos", "hi_p_pos", 100, 0.0, 8.0);     
        hi_p_pos.setTitleX("p (GeV)");
        hi_p_pos.setTitleY("Counts");
        H1F hi_theta_pos = new H1F("hi_theta_pos", "hi_theta_pos", 100, 0.0, 40.0); 
        hi_theta_pos.setTitleX("#theta (deg)");
        hi_theta_pos.setTitleY("Counts");
        H1F hi_phi_pos = new H1F("hi_phi_pos", "hi_phi_pos", 100, -180.0, 180.0);   
        hi_phi_pos.setTitleX("#phi (deg)");
        hi_phi_pos.setTitleY("Counts");
        H1F hi_chi2_pos = new H1F("hi_chi2_pos", "hi_chi2_pos", 100, 0.0, 180.0);   
        hi_chi2_pos.setTitleX("#chi2");
        hi_chi2_pos.setTitleY("Counts");
        H1F hi_vz_pos = new H1F("hi_vz_pos", "hi_vz_pos", 100, -15.0, 15.0);   
        hi_vz_pos.setTitleX("Vz (cm)");
        hi_vz_pos.setTitleY("Counts");
        H1F hi_vz_pos_cut = new H1F("hi_vz_pos_cut", "hi_vz_pos_cut", 100, -15.0, 15.0);   
        hi_vz_pos_cut.setTitleX("Vz (cm)");
        hi_vz_pos_cut.setTitleY("Counts");
        hi_vz_pos_cut.setLineColor(2);
        F1D f1_vz_pos = new F1D("f1_vz_pos","[amp]*gaus(x,[mean],[sigma])", -5.0, 5.0);
        f1_vz_pos.setParameter(0, 0);
        f1_vz_pos.setParameter(1, 0);
        f1_vz_pos.setParameter(2, 1.0);
        f1_vz_pos.setLineWidth(2);
        f1_vz_pos.setLineColor(1);
        f1_vz_pos.setOptStat("1111");
        H2F hi_theta_p_pos = new H2F("hi_theta_p_pos", "hi_theta_p_pos", 100, 0.0, 8.0, 100, 0.0, 40.0); 
        hi_theta_p_pos.setTitleX("p (GeV)");
        hi_theta_p_pos.setTitleY("#theta (deg)");        
        H2F hi_theta_phi_pos = new H2F("hi_theta_phi_pos", "hi_theta_phi_pos", 100, -180.0, 180.0, 100, 0.0, 40.0); 
        hi_theta_phi_pos.setTitleX("#phi (deg)");
        hi_theta_phi_pos.setTitleY("#theta (deg)");  
        H2F hi_chi2_vz_pos = new H2F("hi_chi2_vz_pos", "hi_chi2_vz_pos", 100, -15.0, 15.0, 100, 0.0, 180.0);   
        hi_chi2_vz_pos.setTitleX("Vz (cm)");
        hi_chi2_vz_pos.setTitleY("#chi2");
        DataGroup dg_pos = new DataGroup(4,2);
        dg_pos.addDataSet(hi_p_pos, 0);
        dg_pos.addDataSet(hi_theta_pos, 1);
        dg_pos.addDataSet(hi_phi_pos, 2);
        dg_pos.addDataSet(hi_chi2_pos, 3);
        dg_pos.addDataSet(hi_vz_pos, 4);
        dg_pos.addDataSet(hi_vz_pos_cut, 4);
        dg_pos.addDataSet(f1_vz_pos, 4);
        dg_pos.addDataSet(hi_theta_p_pos, 5);
        dg_pos.addDataSet(hi_theta_phi_pos, 6);
        dg_pos.addDataSet(hi_chi2_vz_pos, 7);
        dataGroups.add(dg_pos, 2);
        // mc comparison
        H1F hi_dp_pos = new H1F("hi_dp_pos", "hi_dp_pos", 100, -0.1, 0.1); 
        hi_dp_pos.setTitleX("#Delta P/P");
        hi_dp_pos.setTitleY("Counts");
        hi_dp_pos.setTitle("TBT Positive Tracks");
        F1D f1_dp_pos = new F1D("f1_dp_pos","[amp]*gaus(x,[mean],[sigma])", -0.5, 0.5);
        f1_dp_pos.setParameter(0, 0);
        f1_dp_pos.setParameter(1, 0);
        f1_dp_pos.setParameter(2, 1.0);
        f1_dp_pos.setLineWidth(2);
        f1_dp_pos.setLineColor(2);
        f1_dp_pos.setOptStat("1111");
        H1F hi_dtheta_pos = new H1F("hi_dtheta_pos","hi_dtheta_pos", 100, -2.0, 2.0); 
        hi_dtheta_pos.setTitleX("#Delta #theta (deg)");
        hi_dtheta_pos.setTitleY("Counts");
        hi_dtheta_pos.setTitle("TBT Positive Tracks");
        F1D f1_dtheta_pos = new F1D("f1_dtheta_pos","[amp]*gaus(x,[mean],[sigma])", -10.0, 10.0);
        f1_dtheta_pos.setParameter(0, 0);
        f1_dtheta_pos.setParameter(1, 0);
        f1_dtheta_pos.setParameter(2, 1.0);
        f1_dtheta_pos.setLineWidth(2);
        f1_dtheta_pos.setLineColor(2);
        f1_dtheta_pos.setOptStat("1111");        
        H1F hi_dphi_pos = new H1F("hi_dphi_pos", "hi_dphi_pos", 100, -8.0, 8.0); 
        hi_dphi_pos.setTitleX("#Delta #phi (deg)");
        hi_dphi_pos.setTitleY("Counts");
        hi_dphi_pos.setTitle("TBT Positive Tracks");
        F1D f1_dphi_pos = new F1D("f1_dphi_pos","[amp]*gaus(x,[mean],[sigma])", -10.0, 10.0);
        f1_dphi_pos.setParameter(0, 0);
        f1_dphi_pos.setParameter(1, 0);
        f1_dphi_pos.setParameter(2, 1.0);
        f1_dphi_pos.setLineWidth(2);
        f1_dphi_pos.setLineColor(2);
        f1_dphi_pos.setOptStat("1111");        
        H1F hi_dvz_pos = new H1F("hi_dvz_pos", "hi_dvz_pos", 100, -20.0, 20.0);   
        hi_dvz_pos.setTitleX("#Delta Vz (cm)");
        hi_dvz_pos.setTitleY("Counts");
        hi_dvz_pos.setTitle("TBT Positive Tracks");
        F1D f1_dvz_pos = new F1D("f1_dvz_pos","[amp]*gaus(x,[mean],[sigma])", -10.0, 10.0);
        f1_dvz_pos.setParameter(0, 0);
        f1_dvz_pos.setParameter(1, 0);
        f1_dvz_pos.setParameter(2, 1.0);
        f1_dvz_pos.setLineWidth(2);
        f1_dvz_pos.setLineColor(2);
        f1_dvz_pos.setOptStat("1111");        
        H1F hi_dp_neg = new H1F("hi_dp_neg", "hi_dp_neg", 100, -0.1, 0.1);
        hi_dp_neg.setTitleX("#Delta P/P");
        hi_dp_neg.setTitleY("Counts");
        hi_dp_neg.setTitle("TBT Negative Tracks");
        F1D f1_dp_neg = new F1D("f1_dp_neg","[amp]*gaus(x,[mean],[sigma])",  -0.5, 0.5);
        f1_dp_neg.setParameter(0, 0);
        f1_dp_neg.setParameter(1, 0);
        f1_dp_neg.setParameter(2, 1.0);
        f1_dp_neg.setLineWidth(2);
        f1_dp_neg.setLineColor(2);
        f1_dp_neg.setOptStat("1111");
        H1F hi_dtheta_neg = new H1F("hi_dtheta_neg","hi_dtheta_neg", 100, -2.0, 2.0); 
        hi_dtheta_neg.setTitleX("#Delta #theta (deg)");
        hi_dtheta_neg.setTitleY("Counts");
        hi_dtheta_neg.setTitle("TBT Negative Tracks");
        F1D f1_dtheta_neg = new F1D("f1_dtheta_neg","[amp]*gaus(x,[mean],[sigma])", -10.0, 10.0);
        f1_dtheta_neg.setParameter(0, 0);
        f1_dtheta_neg.setParameter(1, 0);
        f1_dtheta_neg.setParameter(2, 1.0);
        f1_dtheta_neg.setLineWidth(2);
        f1_dtheta_neg.setLineColor(2);
        f1_dtheta_neg.setOptStat("1111");
        H1F hi_dphi_neg = new H1F("hi_dphi_neg", "hi_dphi_neg", 100, -8.0, 8.0); 
        hi_dphi_neg.setTitleX("#Delta #phi (deg)");
        hi_dphi_neg.setTitleY("Counts");
        hi_dphi_neg.setTitle("TBT Negative Tracks");
        F1D f1_dphi_neg = new F1D("f1_dphi_neg","[amp]*gaus(x,[mean],[sigma])", -10.0, 10.0);
        f1_dphi_neg.setParameter(0, 0);
        f1_dphi_neg.setParameter(1, 0);
        f1_dphi_neg.setParameter(2, 1.0);
        f1_dphi_neg.setLineWidth(2);
        f1_dphi_neg.setLineColor(2);
        f1_dphi_neg.setOptStat("1111");
        H1F hi_dvz_neg = new H1F("hi_dvz_neg", "hi_dvz_neg", 100, -20.0, 20.0);   
        hi_dvz_neg.setTitleX("#Delta Vz (cm)");
        hi_dvz_neg.setTitleY("Counts");
        hi_dvz_neg.setTitle("TBT Negative Tracks");
        F1D f1_dvz_neg = new F1D("f1_dvz_neg","[amp]*gaus(x,[mean],[sigma])", -10.0, 10.0);
        f1_dvz_neg.setParameter(0, 0);
        f1_dvz_neg.setParameter(1, 0);
        f1_dvz_neg.setParameter(2, 1.0);
        f1_dvz_neg.setLineWidth(2);
        f1_dvz_neg.setLineColor(2);
        f1_dvz_neg.setOptStat("1111");
        H2F hi_dp_p_neg = new H2F("hi_dp_p_neg", "hi_dp_p_neg", 16, 0.5, 8.5, 100, -0.2, 0.2);
        hi_dp_p_neg.setTitleX("p");
        hi_dp_p_neg.setTitleY("#Delta p/p");
        hi_dp_p_neg.setTitle("TBT Negative Tracks");
        H2F hi_dp_theta_neg = new H2F("hi_dp_theta_neg", "hi_dp_theta_neg", 30, 5, 35, 100, -0.2, 0.2);
        hi_dp_theta_neg.setTitleX("#theta");
        hi_dp_theta_neg.setTitleY("#Delta p/p");
        hi_dp_theta_neg.setTitle("TBT Negative Tracks");
        H2F hi_dp_phi_neg = new H2F("hi_dp_phi_neg", "hi_dp_phi_neg", 100, -180, 180, 100, -0.2, 0.2);
        hi_dp_phi_neg.setTitleX("#phi");
        hi_dp_phi_neg.setTitleY("#Delta p/p");
        hi_dp_phi_neg.setTitle("TBT Negative Tracks");
        GraphErrors gr_dp_p_neg = new GraphErrors("gr_dp_p_neg");
        gr_dp_p_neg.setTitleX("p");
        gr_dp_p_neg.setTitleY("#Delta p/p");
        gr_dp_p_neg.setTitle("gr_dp_p_neg");
        GraphErrors gr_dp_theta_neg = new GraphErrors("gr_dp_theta_neg");
        gr_dp_theta_neg.setTitleX("#theta");
        gr_dp_theta_neg.setTitleY("#Delta p/p");
        gr_dp_theta_neg.setTitle("gr_dp_p_neg");
        GraphErrors gr_dp_phi_neg = new GraphErrors("gr_dp_phi_neg");
        gr_dp_phi_neg.setTitleX("#phi");
        gr_dp_phi_neg.setTitleY("#Delta p/p");
        gr_dp_phi_neg.setTitle("gr_dp_p_neg");
        DataGroup mc = new DataGroup(4,4);
        mc.addDataSet(hi_dp_pos, 0);
        mc.addDataSet(f1_dp_pos, 0);
        mc.addDataSet(hi_dtheta_pos, 1);
        mc.addDataSet(f1_dtheta_pos, 1);
        mc.addDataSet(hi_dphi_pos, 2);
        mc.addDataSet(f1_dphi_pos, 2);
        mc.addDataSet(hi_dvz_pos, 3);
        mc.addDataSet(f1_dvz_pos, 3);
        mc.addDataSet(hi_dp_neg, 4);
        mc.addDataSet(f1_dp_neg, 4);
        mc.addDataSet(hi_dtheta_neg, 5);
        mc.addDataSet(f1_dtheta_neg, 5);
        mc.addDataSet(hi_dphi_neg, 6);
        mc.addDataSet(f1_dphi_neg, 6);
        mc.addDataSet(hi_dvz_neg, 7);
        mc.addDataSet(f1_dvz_neg, 7);
        mc.addDataSet(hi_dp_p_neg, 8);
        mc.addDataSet(hi_dp_theta_neg, 9);
        mc.addDataSet(hi_dp_phi_neg, 10);
        mc.addDataSet(gr_dp_p_neg, 11);
        mc.addDataSet(gr_dp_theta_neg, 12);
        mc.addDataSet(gr_dp_phi_neg, 13);
        dataGroups.add(mc, 3);
        // vertex
        H2F hi_vxy_pos = new H2F("hi_vxy_pos","hi_vxy_pos",100,-15.,15.,100,-15.,15);
        hi_vxy_pos.setTitleX("Vx (cm)");
        hi_vxy_pos.setTitleY("Vy (cm)");
        H2F hi_vxy_neg = new H2F("hi_vxy_neg","hi_vxy_neg",100,-15.,15.,100,-15.,15);
        hi_vxy_neg.setTitleX("Vx (cm)");
        hi_vxy_neg.setTitleY("Vy (cm)"); 
        H2F hi_vz_vs_theta_pos = new H2F("hi_vz_vs_theta_pos","hi_vz_vs_theta_pos",100, 5.,40.,100,-15.,15);
        hi_vz_vs_theta_pos.setTitleX("#theta (deg)");
        hi_vz_vs_theta_pos.setTitleY("Vz (cm)");
        H2F hi_vz_vs_theta_neg = new H2F("hi_vz_vs_theta_neg","hi_vz_vs_theta_neg",100, 5.,40.,100,-15.,15);
        hi_vz_vs_theta_neg.setTitleX("#theta (deg)");
        hi_vz_vs_theta_neg.setTitleY("Vz (cm)");
	H2F hi_vz_vs_phi_pos = new H2F("hi_vz_vs_phi_pos","hi_vz_vs_phi_pos",200,-15.,15.,200,-180,180);
        hi_vz_vs_phi_pos.setTitleX("Vz (cm)");
        hi_vz_vs_phi_pos.setTitleY("#phi (deg)");
        H2F hi_vz_vs_phi_neg = new H2F("hi_vz_vs_phi_neg","hi_vz_vs_phi_neg",200,-15.,15.,200,-180,180);
        hi_vz_vs_phi_neg.setTitleX("Vz (cm)");
        hi_vz_vs_phi_neg.setTitleY("#phi (deg)");
        DataGroup vertex = new DataGroup(3,2);
        vertex.addDataSet(hi_vz_vs_theta_pos, 0);
        vertex.addDataSet(hi_vxy_pos, 1);
        vertex.addDataSet(hi_vz_vs_phi_pos, 2);
        vertex.addDataSet(hi_vz_vs_theta_neg, 3);
        vertex.addDataSet(hi_vxy_neg, 4);
        vertex.addDataSet(hi_vz_vs_phi_neg, 5);
        dataGroups.add(vertex, 4);

        // CVT Negative Tracks
        H1F hi_p_neg_cvt = new H1F("hi_p_neg", "hi_p_neg", 100, 0.0, 3.0);     
        hi_p_neg_cvt.setTitleX("p (GeV)");
        hi_p_neg_cvt.setTitleY("Counts");
        H1F hi_theta_neg_cvt = new H1F("hi_theta_neg", "hi_theta_neg", 100, 30.0, 150.0); 
        hi_theta_neg_cvt.setTitleX("#theta (deg)");
        hi_theta_neg_cvt.setTitleY("Counts");
        H1F hi_phi_neg_cvt = new H1F("hi_phi_neg", "hi_phi_neg", 100, -180.0, 180.0);   
        hi_phi_neg_cvt.setTitleX("#phi (deg)");
        H1F hi_chi2_neg_cvt = new H1F("hi_chi2_neg", "hi_chi2_neg", 100, 0.0, 180.0);   
        hi_chi2_neg_cvt.setTitleX("#chi2");
        hi_chi2_neg_cvt.setTitleY("Counts");
        H1F hi_vz_neg_cvt = new H1F("hi_vz_neg", "hi_vz_neg", 100, -10.0, 10.0);   
        hi_vz_neg_cvt.setTitleX("Vz (cm)");
        hi_vz_neg_cvt.setTitleY("Counts");
        H2F hi_theta_p_neg_cvt = new H2F("hi_theta_p_neg", "hi_theta_p_neg", 100, 0.0, 3.0, 100, 0.0, 120.0); 
        hi_theta_p_neg_cvt.setTitleX("p (GeV)");
        hi_theta_p_neg_cvt.setTitleY("#theta (deg)");        
        H2F hi_theta_phi_neg_cvt = new H2F("hi_theta_phi_neg", "hi_theta_phi_neg", 100, -180.0, 180.0, 100, 30.0, 150.0); 
        hi_theta_phi_neg_cvt.setTitleX("#phi (deg)");
        hi_theta_phi_neg_cvt.setTitleY("#theta (deg)"); 
        F1D f1_vz_neg_cvt = new F1D("f1_vz_neg","[amp]*gaus(x,[mean],[sigma])", -5.0, 5.0);
        f1_vz_neg_cvt.setParameter(0, 0);
        f1_vz_neg_cvt.setParameter(1, 0);
        f1_vz_neg_cvt.setParameter(2, 1.0);
        f1_vz_neg_cvt.setLineWidth(2);
        f1_vz_neg_cvt.setLineColor(2);
        f1_vz_neg_cvt.setOptStat("1111");
        H2F hi_chi2_vz_neg_cvt = new H2F("hi_chi2_vz_neg", "hi_chi2_vz_neg", 100, -15.0, 15.0, 100, 0.0, 180.0);   
        hi_chi2_vz_neg_cvt.setTitleX("Vz (cm)");
        hi_chi2_vz_neg_cvt.setTitleY("#chi2");        
        DataGroup dg_neg_cvt = new DataGroup(4,2);
        dg_neg_cvt.addDataSet(hi_p_neg_cvt, 0);
        dg_neg_cvt.addDataSet(hi_theta_neg_cvt, 1);
        dg_neg_cvt.addDataSet(hi_phi_neg_cvt, 2);
        dg_neg_cvt.addDataSet(hi_chi2_neg_cvt, 3);
        dg_neg_cvt.addDataSet(hi_vz_neg_cvt, 4);
        dg_neg_cvt.addDataSet(f1_vz_neg_cvt, 4);
        dg_neg_cvt.addDataSet(hi_theta_p_neg_cvt, 5);
        dg_neg_cvt.addDataSet(hi_theta_phi_neg_cvt, 6);
        dg_neg_cvt.addDataSet(hi_chi2_vz_neg_cvt, 7);
        dataGroups.add(dg_neg_cvt, 5);
        // cvt positive trakcs
        H1F hi_p_pos_cvt = new H1F("hi_p_pos", "hi_p_pos", 100, 0.0, 3.0);     
        hi_p_pos_cvt.setTitleX("p (GeV)");
        hi_p_pos_cvt.setTitleY("Counts");
        H1F hi_theta_pos_cvt = new H1F("hi_theta_pos", "hi_theta_pos", 100, 30.0, 150.0); 
        hi_theta_pos_cvt.setTitleX("#theta (deg)");
        hi_theta_pos_cvt.setTitleY("Counts");
        H1F hi_phi_pos_cvt = new H1F("hi_phi_pos", "hi_phi_pos", 100, -180.0, 180.0);   
        hi_phi_pos_cvt.setTitleX("#phi (deg)");
        H1F hi_chi2_pos_cvt = new H1F("hi_chi2_pos", "hi_chi2_pos", 100, 0.0, 180.0);   
        hi_chi2_pos_cvt.setTitleX("#chi2");
        hi_chi2_pos_cvt.setTitleY("Counts");
        H1F hi_vz_pos_cvt = new H1F("hi_vz_pos", "hi_vz_pos", 100, -10.0, 10.0);   
        hi_vz_pos_cvt.setTitleX("Vz (cm)");
        hi_vz_pos_cvt.setTitleY("Counts");
        H2F hi_theta_p_pos_cvt = new H2F("hi_theta_p_pos", "hi_theta_p_pos", 100, 0.0, 3.0, 100, 0.0, 120.0); 
        hi_theta_p_pos_cvt.setTitleX("p (GeV)");
        hi_theta_p_pos_cvt.setTitleY("#theta (deg)");        
        H2F hi_theta_phi_pos_cvt = new H2F("hi_theta_phi_pos", "hi_theta_phi_pos", 100, -180.0, 180.0, 100, 30.0, 150.0); 
        hi_theta_phi_pos_cvt.setTitleX("#phi (deg)");
        hi_theta_phi_pos_cvt.setTitleY("#theta (deg)");
        F1D f1_vz_pos_cvt = new F1D("f1_vz_pos","[amp]*gaus(x,[mean],[sigma])", -5.0, 5.0);
        f1_vz_pos_cvt.setParameter(0, 0);
        f1_vz_pos_cvt.setParameter(1, 0);
        f1_vz_pos_cvt.setParameter(2, 1.0);
        f1_vz_pos_cvt.setLineWidth(2);
        f1_vz_pos_cvt.setLineColor(2);
        f1_vz_pos_cvt.setOptStat("1111");  
        H2F hi_chi2_vz_pos_cvt = new H2F("hi_chi2_vz_pos", "hi_chi2_vz_pos", 100, -15.0, 15.0, 100, 0.0, 180.0);   
        hi_chi2_vz_pos_cvt.setTitleX("Vz (cm)");
        hi_chi2_vz_pos_cvt.setTitleY("#chi2");        
        DataGroup dg_pos_cvt = new DataGroup(4,4);
        dg_pos_cvt.addDataSet(hi_p_pos_cvt, 0);
        dg_pos_cvt.addDataSet(hi_theta_pos_cvt, 1);
        dg_pos_cvt.addDataSet(hi_phi_pos_cvt, 2);
        dg_pos_cvt.addDataSet(hi_chi2_pos_cvt, 3);
        dg_pos_cvt.addDataSet(hi_vz_pos_cvt, 4);
        dg_pos_cvt.addDataSet(f1_vz_pos_cvt, 4);
        dg_pos_cvt.addDataSet(hi_theta_p_pos_cvt, 5);
        dg_pos_cvt.addDataSet(hi_theta_phi_pos_cvt, 6);
        dg_pos_cvt.addDataSet(hi_chi2_vz_pos_cvt, 7);
        dataGroups.add(dg_pos_cvt, 6);
        // mc CVT comparison
        H1F hi_dp_pos_cvt = new H1F("hi_dp_pos", "hi_dp_pos", 100, -0.2, 0.2); 
        hi_dp_pos_cvt.setTitleX("#Delta P/P");
        hi_dp_pos_cvt.setTitleY("Counts");
        hi_dp_pos_cvt.setTitle("CVT Positive Tracks");
        F1D f1_dp_pos_cvt = new F1D("f1_dp_pos","[amp]*gaus(x,[mean],[sigma])", -0.5, 0.5);
        f1_dp_pos_cvt.setParameter(0, 0);
        f1_dp_pos_cvt.setParameter(1, 0);
        f1_dp_pos_cvt.setParameter(2, 1.0);
        f1_dp_pos_cvt.setLineWidth(2);
        f1_dp_pos_cvt.setLineColor(2);
        f1_dp_pos_cvt.setOptStat("1111");
        H1F hi_dtheta_pos_cvt = new H1F("hi_dtheta_pos","hi_dtheta_pos", 100, -5.0, 5.0); 
        hi_dtheta_pos_cvt.setTitleX("#Delta #theta (deg)");
        hi_dtheta_pos_cvt.setTitleY("Counts");
        hi_dtheta_pos_cvt.setTitle("CVT Positive Tracks");
        F1D f1_dtheta_pos_cvt = new F1D("f1_dtheta_pos","[amp]*gaus(x,[mean],[sigma])", -10.0, 10.0);
        f1_dtheta_pos_cvt.setParameter(0, 0);
        f1_dtheta_pos_cvt.setParameter(1, 0);
        f1_dtheta_pos_cvt.setParameter(2, 1.0);
        f1_dtheta_pos_cvt.setLineWidth(2);
        f1_dtheta_pos_cvt.setLineColor(2);
        f1_dtheta_pos_cvt.setOptStat("1111");        
        H1F hi_dphi_pos_cvt = new H1F("hi_dphi_pos", "hi_dphi_pos", 100, -5.0, 5.0); 
        hi_dphi_pos_cvt.setTitleX("#Delta #phi (deg)");
        hi_dphi_pos_cvt.setTitleY("Counts");
        hi_dphi_pos_cvt.setTitle("CVT Positive Tracks");
        F1D f1_dphi_pos_cvt = new F1D("f1_dphi_pos","[amp]*gaus(x,[mean],[sigma])", -10.0, 10.0);
        f1_dphi_pos_cvt.setParameter(0, 0);
        f1_dphi_pos_cvt.setParameter(1, 0);
        f1_dphi_pos_cvt.setParameter(2, 1.0);
        f1_dphi_pos_cvt.setLineWidth(2);
        f1_dphi_pos_cvt.setLineColor(2);
        f1_dphi_pos_cvt.setOptStat("1111");        
        H1F hi_dvz_pos_cvt = new H1F("hi_dvz_pos", "hi_dvz_pos", 100, -5.0, 5.0);   
        hi_dvz_pos_cvt.setTitleX("#Delta Vz (cm)");
        hi_dvz_pos_cvt.setTitleY("Counts");
        hi_dvz_pos_cvt.setTitle("CVT Positive Tracks");
        F1D f1_dvz_pos_cvt = new F1D("f1_dvz_pos","[amp]*gaus(x,[mean],[sigma])", -10.0, 10.0);
        f1_dvz_pos_cvt.setParameter(0, 0);
        f1_dvz_pos_cvt.setParameter(1, 0);
        f1_dvz_pos_cvt.setParameter(2, 1.0);
        f1_dvz_pos_cvt.setLineWidth(2);
        f1_dvz_pos_cvt.setLineColor(2);
        f1_dvz_pos_cvt.setOptStat("1111");        
        H1F hi_dp_neg_cvt = new H1F("hi_dp_neg", "hi_dp_neg", 100, -0.2, 0.2);
        hi_dp_neg_cvt.setTitleX("#Delta P/P");
        hi_dp_neg_cvt.setTitleY("Counts");
        hi_dp_neg_cvt.setTitle("CVT Negative Tracks");
        F1D f1_dp_neg_cvt = new F1D("f1_dp_neg","[amp]*gaus(x,[mean],[sigma])",  -0.5, 0.5);
        f1_dp_neg_cvt.setParameter(0, 0);
        f1_dp_neg_cvt.setParameter(1, 0);
        f1_dp_neg_cvt.setParameter(2, 1.0);
        f1_dp_neg_cvt.setLineWidth(2);
        f1_dp_neg_cvt.setLineColor(2);
        f1_dp_neg_cvt.setOptStat("1111");
        H1F hi_dtheta_neg_cvt = new H1F("hi_dtheta_neg","hi_dtheta_neg", 100, -5.0, 5.0); 
        hi_dtheta_neg_cvt.setTitleX("#Delta #theta (deg)");
        hi_dtheta_neg_cvt.setTitleY("Counts");
        hi_dtheta_neg_cvt.setTitle("CVT Negative Tracks");
        F1D f1_dtheta_neg_cvt = new F1D("f1_dtheta_neg","[amp]*gaus(x,[mean],[sigma])", -10.0, 10.0);
        f1_dtheta_neg_cvt.setParameter(0, 0);
        f1_dtheta_neg_cvt.setParameter(1, 0);
        f1_dtheta_neg_cvt.setParameter(2, 1.0);
        f1_dtheta_neg_cvt.setLineWidth(2);
        f1_dtheta_neg_cvt.setLineColor(2);
        f1_dtheta_neg_cvt.setOptStat("1111");
        H1F hi_dphi_neg_cvt = new H1F("hi_dphi_neg", "hi_dphi_neg", 100, -5.0, 5.0); 
        hi_dphi_neg_cvt.setTitleX("#Delta #phi (deg)");
        hi_dphi_neg_cvt.setTitleY("Counts");
        hi_dphi_neg_cvt.setTitle("CVT Negative Tracks");
        F1D f1_dphi_neg_cvt = new F1D("f1_dphi_neg","[amp]*gaus(x,[mean],[sigma])", -10.0, 10.0);
        f1_dphi_neg_cvt.setParameter(0, 0);
        f1_dphi_neg_cvt.setParameter(1, 0);
        f1_dphi_neg_cvt.setParameter(2, 1.0);
        f1_dphi_neg_cvt.setLineWidth(2);
        f1_dphi_neg_cvt.setLineColor(2);
        f1_dphi_neg_cvt.setOptStat("1111");
        H1F hi_dvz_neg_cvt = new H1F("hi_dvz_neg", "hi_dvz_neg", 100, -5.0, 5.0);   
        hi_dvz_neg_cvt.setTitleX("#Delta Vz (cm)");
        hi_dvz_neg_cvt.setTitleY("Counts");
        hi_dvz_neg_cvt.setTitle("CVT Negative Tracks");
        F1D f1_dvz_neg_cvt = new F1D("f1_dvz_neg","[amp]*gaus(x,[mean],[sigma])", -10.0, 10.0);
        f1_dvz_neg_cvt.setParameter(0, 0);
        f1_dvz_neg_cvt.setParameter(1, 0);
        f1_dvz_neg_cvt.setParameter(2, 1.0);
        f1_dvz_neg_cvt.setLineWidth(2);
        f1_dvz_neg_cvt.setLineColor(2);
        f1_dvz_neg_cvt.setOptStat("1111");
        DataGroup mc_cvt = new DataGroup(4,2);
        mc_cvt.addDataSet(hi_dp_pos_cvt, 0);
        mc_cvt.addDataSet(f1_dp_pos_cvt, 0);
        mc_cvt.addDataSet(hi_dtheta_pos_cvt, 1);
        mc_cvt.addDataSet(f1_dtheta_pos_cvt, 1);
        mc_cvt.addDataSet(hi_dphi_pos_cvt, 2);
        mc_cvt.addDataSet(f1_dphi_pos_cvt, 2);
        mc_cvt.addDataSet(hi_dvz_pos_cvt, 3);
        mc_cvt.addDataSet(f1_dvz_pos_cvt, 3);
        mc_cvt.addDataSet(hi_dp_neg_cvt, 4);
        mc_cvt.addDataSet(f1_dp_neg_cvt, 4);
        mc_cvt.addDataSet(hi_dtheta_neg_cvt, 5);
        mc_cvt.addDataSet(f1_dtheta_neg_cvt, 5);
        mc_cvt.addDataSet(hi_dphi_neg_cvt, 6);
        mc_cvt.addDataSet(f1_dphi_neg_cvt, 6);
        mc_cvt.addDataSet(hi_dvz_neg_cvt, 7);
        mc_cvt.addDataSet(f1_dvz_neg_cvt, 7);
        dataGroups.add(mc_cvt, 7);
        //CVT Vertex
        H2F hi_vxy_pos_cvt = new H2F("hi_vxy_pos","hi_vxy_pos",100,-.25,.25,100,-.25,.25);
        hi_vxy_pos_cvt.setTitleX("Vx (cm)");
        hi_vxy_pos_cvt.setTitleY("Vy (cm)");
        H2F hi_vxy_neg_cvt = new H2F("hi_vxy_neg","hi_vxy_neg",100,-.25,.25,100,-.25,.25);
        hi_vxy_neg_cvt.setTitleX("Vx (cm)");
        hi_vxy_neg_cvt.setTitleY("Vy (cm)"); 
        H2F hi_vz_vs_theta_pos_cvt = new H2F("hi_vz_vs_theta_pos","hi_vz_vs_theta_pos",100, 5.,120.,100,-15.,15);
        hi_vz_vs_theta_pos_cvt.setTitleX("#theta (deg)");
        hi_vz_vs_theta_pos_cvt.setTitleY("Vz (cm)");
        H2F hi_vz_vs_theta_neg_cvt = new H2F("hi_vz_vs_theta_neg","hi_vz_vs_theta_neg",100, 5.,120.,100,-15.,15);
        hi_vz_vs_theta_neg_cvt.setTitleX("#theta (deg)");
        hi_vz_vs_theta_neg_cvt.setTitleY("Vz (cm)");
        H2F hi_vz_vs_phi_pos_cvt = new H2F("hi_vz_vs_phi_pos","hi_vz_vs_phi_pos",200,-15.,15.,200,-180,180);
        hi_vz_vs_phi_pos_cvt.setTitleX("Vz (cm)");
        hi_vz_vs_phi_pos_cvt.setTitleY("#phi (deg)");
        H2F hi_vz_vs_phi_neg_cvt = new H2F("hi_vz_vs_phi_neg","hi_vz_vs_phi_neg",200,-15.,15.,200,-180,180);
        hi_vz_vs_phi_neg_cvt.setTitleX("Vz (cm)");
        hi_vz_vs_phi_neg_cvt.setTitleY("#phi (deg)");
        DataGroup cvtVertex = new DataGroup(3,2);
        cvtVertex.addDataSet(hi_vz_vs_theta_pos_cvt, 0);
        cvtVertex.addDataSet(hi_vxy_pos_cvt,         1);
        cvtVertex.addDataSet(hi_vz_vs_phi_pos_cvt,   2);
        cvtVertex.addDataSet(hi_vz_vs_theta_neg_cvt, 3);
        cvtVertex.addDataSet(hi_vxy_neg_cvt,         4);
        cvtVertex.addDataSet(hi_vz_vs_phi_neg_cvt,   5);
        dataGroups.add(cvtVertex, 8);

        // negative and positive tracks by sector
                // positive particles 
        DataGroup dg_pos_sec = new DataGroup(3,2);
        DataGroup dg_neg_sec = new DataGroup(3,2);
        for(int sector=1; sector <= 6; sector++) {
            H1F hi_vz_pos_sec = new H1F("hi_vz_pos_sec_" + sector, "hi_vz_pos_sec_" + sector, 100, -15.0, 15.0);   
            hi_vz_pos_sec.setTitleX("Vz (cm)");
            hi_vz_pos_sec.setTitleY("Counts");
            H1F hi_vz_pos_sec_cut = new H1F("hi_vz_pos_sec_" + sector + "_cut", "hi_vz_pos_sec_" + sector + "_cut", 100, -15.0, 15.0);   
            hi_vz_pos_sec_cut.setTitleX("Vz (cm)");
            hi_vz_pos_sec_cut.setTitleY("Counts");
            hi_vz_pos_sec_cut.setLineColor(2);
            F1D f1_vz_pos_sec = new F1D("f1_vz_pos_sec_" + sector,"[amp]*gaus(x,[mean],[sigma])", -5.0, 5.0);
            f1_vz_pos_sec.setParameter(0, 0);
            f1_vz_pos_sec.setParameter(1, 0);
            f1_vz_pos_sec.setParameter(2, 1.0);
            f1_vz_pos_sec.setLineWidth(2);
            f1_vz_pos_sec.setLineColor(1);
            f1_vz_pos_sec.setOptStat("1111");
            dg_pos_sec.addDataSet(hi_vz_pos_sec, sector-1);
            dg_pos_sec.addDataSet(hi_vz_pos_sec_cut, sector-1);
            dg_pos_sec.addDataSet(f1_vz_pos_sec, sector-1);
            H1F hi_vz_neg_sec = new H1F("hi_vz_neg_sec_" + sector, "hi_vz_neg_sec_" + sector, 100, -15.0, 15.0);   
            hi_vz_neg_sec.setTitleX("Vz (cm)");
            hi_vz_neg_sec.setTitleY("Counts");
            H1F hi_vz_neg_sec_cut = new H1F("hi_vz_neg_sec_" + sector + "_cut", "hi_vz_neg_sec_" + sector + "_cut", 100, -15.0, 15.0);   
            hi_vz_neg_sec_cut.setTitleX("Vz (cm)");
            hi_vz_neg_sec_cut.setTitleY("Counts");
            hi_vz_neg_sec_cut.setLineColor(2);
            F1D f1_vz_neg_sec = new F1D("f1_vz_neg_sec_" + sector,"[amp]*gaus(x,[mean],[sigma])", -5.0, 5.0);
            f1_vz_neg_sec.setParameter(0, 0);
            f1_vz_neg_sec.setParameter(1, 0);
            f1_vz_neg_sec.setParameter(2, 1.0);
            f1_vz_neg_sec.setLineWidth(2);
            f1_vz_neg_sec.setLineColor(1);
            f1_vz_neg_sec.setOptStat("1111");
            dg_neg_sec.addDataSet(hi_vz_neg_sec, sector-1);
            dg_neg_sec.addDataSet(hi_vz_neg_sec_cut, sector-1);
            dg_neg_sec.addDataSet(f1_vz_neg_sec, sector-1); 
        }
        dataGroups.add(dg_pos_sec, 9);
        dataGroups.add(dg_neg_sec, 10);


        H2F vertex_CVT_TBT_pos = new H2F("vertex_CVT_TBT_pos","vertex_CVT_TBT_pos",100, -20.,20.,100,-15.,15);
        vertex_CVT_TBT_pos.setTitleX("Vz CVT (cm)");
        vertex_CVT_TBT_pos.setTitleY("Vz TBT (cm)");
        H1F vertex_CVT_TBT_diff_pos = new H1F("vertex_CVT_TBT_diff_pos", "vertex_CVT_TBT_diff_pos", 100, -15., 15.);     
        vertex_CVT_TBT_diff_pos.setTitleX("Vz (cm)");
        vertex_CVT_TBT_diff_pos.setTitleY("Counts");
        H2F vertex_CVT_TBT_neg = new H2F("vertex_CVT_TBT_neg","vertex_CVT_TBT_neg",100, -20.,20.,100,-15.,15);
        vertex_CVT_TBT_neg.setTitleX("Vz CVT (cm)");
        vertex_CVT_TBT_neg.setTitleY("Vz TBT (cm)");
        H1F vertex_CVT_TBT_diff_neg = new H1F("vertex_CVT_TBT_diff_neg", "vertex_CVT_TBT_diff_neg", 100, -15., 15.);     
        vertex_CVT_TBT_diff_neg.setTitleX("Vz (cm)");
        vertex_CVT_TBT_diff_neg.setTitleY("Counts");
        DataGroup vertex_comparison = new DataGroup(2,2);
        vertex_comparison.addDataSet(vertex_CVT_TBT_pos, 0);
        vertex_comparison.addDataSet(vertex_CVT_TBT_diff_pos, 1);
        vertex_comparison.addDataSet(vertex_CVT_TBT_neg, 2);
        vertex_comparison.addDataSet(vertex_CVT_TBT_diff_neg, 3);
        dataGroups.add(vertex_comparison, 11);

    }

    private void plotHistos() {
        canvasTabbed.getCanvas("TBT Negative Tracks").divide(4,2);
        canvasTabbed.getCanvas("TBT Negative Tracks").setGridX(false);
        canvasTabbed.getCanvas("TBT Negative Tracks").setGridY(false);
        canvasTabbed.getCanvas("TBT Positive Tracks").divide(4,2);
        canvasTabbed.getCanvas("TBT Positive Tracks").setGridX(false);
        canvasTabbed.getCanvas("TBT Positive Tracks").setGridY(false);
        canvasTabbed.getCanvas("TBT Resolution").divide(4, 2);
        canvasTabbed.getCanvas("TBT Resolution").setGridX(false);
        canvasTabbed.getCanvas("TBT Resolution").setGridY(false);
        canvasTabbed.getCanvas("TBT negative part. resolution").divide(3, 2);
        canvasTabbed.getCanvas("TBT negative part. resolution").setGridX(false);
        canvasTabbed.getCanvas("TBT negative part. resolution").setGridY(false);
        canvasTabbed.getCanvas("TBT Vertex").divide(4, 2);
        canvasTabbed.getCanvas("TBT Vertex").setGridX(false);
        canvasTabbed.getCanvas("TBT Vertex").setGridY(false);
        
        canvasTabbed.getCanvas("CVT Negative Tracks").divide(4,2);
        canvasTabbed.getCanvas("CVT Negative Tracks").setGridX(false);
        canvasTabbed.getCanvas("CVT Negative Tracks").setGridY(false);        
        canvasTabbed.getCanvas("CVT Positive Tracks").divide(4,2);
        canvasTabbed.getCanvas("CVT Positive Tracks").setGridX(false);
        canvasTabbed.getCanvas("CVT Positive Tracks").setGridY(false);
        canvasTabbed.getCanvas("CVT Resolution").divide(4, 2);
        canvasTabbed.getCanvas("CVT Resolution").setGridX(false);
        canvasTabbed.getCanvas("CVT Resolution").setGridY(false);
        canvasTabbed.getCanvas("CVT Vertex").divide(4, 2);
        canvasTabbed.getCanvas("CVT Vertex").setGridX(false);
        canvasTabbed.getCanvas("CVT Vertex").setGridY(false);


        canvasTabbed.getCanvas("TBT Positive Tracks by Sector").divide(3,2);
        canvasTabbed.getCanvas("TBT Positive Tracks by Sector").setGridX(false);
        canvasTabbed.getCanvas("TBT Positive Tracks by Sector").setGridY(false);

        canvasTabbed.getCanvas("TBT Negative Tracks by Sector").divide(3,2);
        canvasTabbed.getCanvas("TBT Negative Tracks by Sector").setGridX(false);
        canvasTabbed.getCanvas("TBT Negative Tracks by Sector").setGridY(false);


        canvasTabbed.getCanvas("TBT-CVT Vertex Comparison").divide(2, 2);
        canvasTabbed.getCanvas("TBT-CVT Vertex Comparison").setGridX(false);
        canvasTabbed.getCanvas("TBT-CVT Vertex Comparison").setGridY(false);


        canvasTabbed.getCanvas("TBT Negative Tracks").cd(0);
        canvasTabbed.getCanvas("TBT Negative Tracks").draw(dataGroups.getItem(1).getH1F("hi_p_neg"));
        canvasTabbed.getCanvas("TBT Negative Tracks").cd(1);
        canvasTabbed.getCanvas("TBT Negative Tracks").draw(dataGroups.getItem(1).getH1F("hi_theta_neg"));
        canvasTabbed.getCanvas("TBT Negative Tracks").cd(2);
        canvasTabbed.getCanvas("TBT Negative Tracks").draw(dataGroups.getItem(1).getH1F("hi_phi_neg"));
        canvasTabbed.getCanvas("TBT Negative Tracks").cd(3);
        canvasTabbed.getCanvas("TBT Negative Tracks").getPad(3).getAxisY().setLog(true);
        canvasTabbed.getCanvas("TBT Negative Tracks").draw(dataGroups.getItem(1).getH1F("hi_chi2_neg"));
        canvasTabbed.getCanvas("TBT Negative Tracks").cd(4);
        canvasTabbed.getCanvas("TBT Negative Tracks").draw(dataGroups.getItem(1).getH1F("hi_vz_neg"));
        canvasTabbed.getCanvas("TBT Negative Tracks").draw(dataGroups.getItem(1).getH1F("hi_vz_neg_cut"),"same");
        canvasTabbed.getCanvas("TBT Negative Tracks").draw(dataGroups.getItem(1).getF1D("f1_vz_neg"),"same");
        canvasTabbed.getCanvas("TBT Negative Tracks").cd(5);
        canvasTabbed.getCanvas("TBT Negative Tracks").draw(dataGroups.getItem(1).getH2F("hi_theta_p_neg"));
        canvasTabbed.getCanvas("TBT Negative Tracks").cd(6);
        canvasTabbed.getCanvas("TBT Negative Tracks").draw(dataGroups.getItem(1).getH2F("hi_theta_phi_neg"));
        canvasTabbed.getCanvas("TBT Negative Tracks").cd(7);
        canvasTabbed.getCanvas("TBT Negative Tracks").draw(dataGroups.getItem(1).getH2F("hi_chi2_vz_neg"));
        canvasTabbed.getCanvas("TBT Positive Tracks").cd(0);
        canvasTabbed.getCanvas("TBT Positive Tracks").draw(dataGroups.getItem(2).getH1F("hi_p_pos"));
        canvasTabbed.getCanvas("TBT Positive Tracks").cd(1);
        canvasTabbed.getCanvas("TBT Positive Tracks").draw(dataGroups.getItem(2).getH1F("hi_theta_pos"));
        canvasTabbed.getCanvas("TBT Positive Tracks").cd(2);
        canvasTabbed.getCanvas("TBT Positive Tracks").draw(dataGroups.getItem(2).getH1F("hi_phi_pos"));
        canvasTabbed.getCanvas("TBT Positive Tracks").cd(3);
        canvasTabbed.getCanvas("TBT Positive Tracks").getPad(3).getAxisY().setLog(true);
        canvasTabbed.getCanvas("TBT Positive Tracks").draw(dataGroups.getItem(2).getH1F("hi_chi2_pos"));
        canvasTabbed.getCanvas("TBT Positive Tracks").cd(4);
        canvasTabbed.getCanvas("TBT Positive Tracks").draw(dataGroups.getItem(2).getH1F("hi_vz_pos"));
        canvasTabbed.getCanvas("TBT Positive Tracks").draw(dataGroups.getItem(2).getH1F("hi_vz_pos_cut"),"same");
        canvasTabbed.getCanvas("TBT Positive Tracks").draw(dataGroups.getItem(2).getF1D("f1_vz_pos"),"same");
        canvasTabbed.getCanvas("TBT Positive Tracks").cd(5);
        canvasTabbed.getCanvas("TBT Positive Tracks").draw(dataGroups.getItem(2).getH2F("hi_theta_p_pos"));
        canvasTabbed.getCanvas("TBT Positive Tracks").cd(6);
        canvasTabbed.getCanvas("TBT Positive Tracks").draw(dataGroups.getItem(2).getH2F("hi_theta_phi_pos"));        
        canvasTabbed.getCanvas("TBT Positive Tracks").cd(7);
        canvasTabbed.getCanvas("TBT Positive Tracks").draw(dataGroups.getItem(2).getH2F("hi_chi2_vz_pos"));
        canvasTabbed.getCanvas("TBT Resolution").cd(0);
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getH1F("hi_dp_pos"));
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getF1D("f1_dp_pos"),"same");
        canvasTabbed.getCanvas("TBT Resolution").cd(1);
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getH1F("hi_dtheta_pos"));
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getF1D("f1_dtheta_pos"),"same");
        canvasTabbed.getCanvas("TBT Resolution").cd(2);
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getH1F("hi_dphi_pos"));
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getF1D("f1_dphi_pos"),"same");
        canvasTabbed.getCanvas("TBT Resolution").cd(3);
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getH1F("hi_dvz_pos"));
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getF1D("f1_dvz_pos"),"same");
        canvasTabbed.getCanvas("TBT Resolution").cd(4);
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getH1F("hi_dp_neg"));
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getF1D("f1_dp_neg"),"same");
        canvasTabbed.getCanvas("TBT Resolution").cd(5);
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getH1F("hi_dtheta_neg"));
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getF1D("f1_dtheta_neg"),"same");
        canvasTabbed.getCanvas("TBT Resolution").cd(6);
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getH1F("hi_dphi_neg"));
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getF1D("f1_dphi_neg"),"same");
        canvasTabbed.getCanvas("TBT Resolution").cd(7);
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getH1F("hi_dvz_neg"));
        canvasTabbed.getCanvas("TBT Resolution").draw(dataGroups.getItem(3).getF1D("f1_dvz_neg"),"same");
        canvasTabbed.getCanvas("TBT negative part. resolution").cd(0);
        canvasTabbed.getCanvas("TBT negative part. resolution").draw(dataGroups.getItem(3).getH2F("hi_dp_p_neg"));
        canvasTabbed.getCanvas("TBT negative part. resolution").cd(1);
        canvasTabbed.getCanvas("TBT negative part. resolution").draw(dataGroups.getItem(3).getH2F("hi_dp_theta_neg"));
        canvasTabbed.getCanvas("TBT negative part. resolution").cd(2);
        canvasTabbed.getCanvas("TBT negative part. resolution").draw(dataGroups.getItem(3).getH2F("hi_dp_phi_neg"));
        canvasTabbed.getCanvas("TBT negative part. resolution").cd(3);
        canvasTabbed.getCanvas("TBT negative part. resolution").getPad(3).getAxisY().setRange(-0.05, 0.05);
        if(dataGroups.getItem(3).getGraph("gr_dp_p_neg").getDataSize(0)>1) canvasTabbed.getCanvas("TBT negative part. resolution").draw(dataGroups.getItem(3).getGraph("gr_dp_p_neg"));
        canvasTabbed.getCanvas("TBT negative part. resolution").cd(4);
        canvasTabbed.getCanvas("TBT negative part. resolution").getPad(4).getAxisY().setRange(-0.05, 0.05);
        if(dataGroups.getItem(3).getGraph("gr_dp_theta_neg").getDataSize(0)>1) canvasTabbed.getCanvas("TBT negative part. resolution").draw(dataGroups.getItem(3).getGraph("gr_dp_theta_neg"));
        canvasTabbed.getCanvas("TBT negative part. resolution").cd(5);
        canvasTabbed.getCanvas("TBT negative part. resolution").getPad(5).getAxisY().setRange(-0.05, 0.05);
        if(dataGroups.getItem(3).getGraph("gr_dp_phi_neg").getDataSize(0)>1) canvasTabbed.getCanvas("TBT negative part. resolution").draw(dataGroups.getItem(3).getGraph("gr_dp_phi_neg"));
        canvasTabbed.getCanvas("TBT Vertex").cd(0);
        canvasTabbed.getCanvas("TBT Vertex").draw(dataGroups.getItem(2).getH1F("hi_vz_pos"));
        canvasTabbed.getCanvas("TBT Vertex").draw(dataGroups.getItem(2).getH1F("hi_vz_pos_cut"),"same");
        canvasTabbed.getCanvas("TBT Vertex").draw(dataGroups.getItem(2).getF1D("f1_vz_pos"),"same");
        canvasTabbed.getCanvas("TBT Vertex").cd(1);
        canvasTabbed.getCanvas("TBT Vertex").draw(dataGroups.getItem(4).getH2F("hi_vz_vs_theta_pos"));
        canvasTabbed.getCanvas("TBT Vertex").cd(2);
        canvasTabbed.getCanvas("TBT Vertex").getPad(2).getAxisZ().setLog(true);
        canvasTabbed.getCanvas("TBT Vertex").draw(dataGroups.getItem(4).getH2F("hi_vxy_pos"));
        canvasTabbed.getCanvas("TBT Vertex").cd(3);
        canvasTabbed.getCanvas("TBT Vertex").draw(dataGroups.getItem(4).getH2F("hi_vz_vs_phi_pos"));
        canvasTabbed.getCanvas("TBT Vertex").cd(4);
        canvasTabbed.getCanvas("TBT Vertex").draw(dataGroups.getItem(1).getH1F("hi_vz_neg"));
        canvasTabbed.getCanvas("TBT Vertex").draw(dataGroups.getItem(1).getH1F("hi_vz_neg_cut"),"same");
        canvasTabbed.getCanvas("TBT Vertex").draw(dataGroups.getItem(1).getF1D("f1_vz_neg"),"same");
        canvasTabbed.getCanvas("TBT Vertex").cd(5);
        canvasTabbed.getCanvas("TBT Vertex").draw(dataGroups.getItem(4).getH2F("hi_vz_vs_theta_neg"));
        canvasTabbed.getCanvas("TBT Vertex").cd(6);
        canvasTabbed.getCanvas("TBT Vertex").getPad(6).getAxisZ().setLog(true);
        canvasTabbed.getCanvas("TBT Vertex").draw(dataGroups.getItem(4).getH2F("hi_vxy_neg"));
        canvasTabbed.getCanvas("TBT Vertex").cd(7);
        canvasTabbed.getCanvas("TBT Vertex").draw(dataGroups.getItem(4).getH2F("hi_vz_vs_phi_neg"));

        canvasTabbed.getCanvas("CVT Negative Tracks").cd(0);
        canvasTabbed.getCanvas("CVT Negative Tracks").draw(dataGroups.getItem(5).getH1F("hi_p_neg"));
        canvasTabbed.getCanvas("CVT Negative Tracks").cd(1);
        canvasTabbed.getCanvas("CVT Negative Tracks").draw(dataGroups.getItem(5).getH1F("hi_theta_neg"));
        canvasTabbed.getCanvas("CVT Negative Tracks").cd(2);
        canvasTabbed.getCanvas("CVT Negative Tracks").draw(dataGroups.getItem(5).getH1F("hi_phi_neg"));
        canvasTabbed.getCanvas("CVT Negative Tracks").cd(3);
        canvasTabbed.getCanvas("CVT Negative Tracks").getPad(3).getAxisY().setLog(true);
        canvasTabbed.getCanvas("CVT Negative Tracks").draw(dataGroups.getItem(5).getH1F("hi_chi2_neg"));
        canvasTabbed.getCanvas("CVT Negative Tracks").cd(4);
        canvasTabbed.getCanvas("CVT Negative Tracks").draw(dataGroups.getItem(5).getH1F("hi_vz_neg"));
        canvasTabbed.getCanvas("CVT Negative Tracks").draw(dataGroups.getItem(5).getF1D("f1_vz_neg"),"same");
        canvasTabbed.getCanvas("CVT Negative Tracks").cd(5);
        canvasTabbed.getCanvas("CVT Negative Tracks").draw(dataGroups.getItem(5).getH2F("hi_theta_p_neg"));
        canvasTabbed.getCanvas("CVT Negative Tracks").cd(6);
        canvasTabbed.getCanvas("CVT Negative Tracks").draw(dataGroups.getItem(5).getH2F("hi_theta_phi_neg"));
        canvasTabbed.getCanvas("CVT Negative Tracks").cd(7);
        canvasTabbed.getCanvas("CVT Negative Tracks").draw(dataGroups.getItem(5).getH2F("hi_chi2_vz_neg"));
        
        canvasTabbed.getCanvas("CVT Positive Tracks").cd(0);
        canvasTabbed.getCanvas("CVT Positive Tracks").draw(dataGroups.getItem(6).getH1F("hi_p_pos"));
        canvasTabbed.getCanvas("CVT Positive Tracks").cd(1);
        canvasTabbed.getCanvas("CVT Positive Tracks").draw(dataGroups.getItem(6).getH1F("hi_theta_pos"));
        canvasTabbed.getCanvas("CVT Positive Tracks").cd(2);
        canvasTabbed.getCanvas("CVT Positive Tracks").draw(dataGroups.getItem(6).getH1F("hi_phi_pos"));
        canvasTabbed.getCanvas("CVT Positive Tracks").cd(3);
        canvasTabbed.getCanvas("CVT Positive Tracks").getPad(3).getAxisY().setLog(true);
        canvasTabbed.getCanvas("CVT Positive Tracks").draw(dataGroups.getItem(6).getH1F("hi_chi2_pos"));
        canvasTabbed.getCanvas("CVT Positive Tracks").cd(4);
        canvasTabbed.getCanvas("CVT Positive Tracks").draw(dataGroups.getItem(6).getH1F("hi_vz_pos"));
        canvasTabbed.getCanvas("CVT Positive Tracks").draw(dataGroups.getItem(6).getF1D("f1_vz_pos"),"same");
        canvasTabbed.getCanvas("CVT Positive Tracks").cd(5);
        canvasTabbed.getCanvas("CVT Positive Tracks").draw(dataGroups.getItem(6).getH2F("hi_theta_p_pos"));
        canvasTabbed.getCanvas("CVT Positive Tracks").cd(6);
        canvasTabbed.getCanvas("CVT Positive Tracks").draw(dataGroups.getItem(6).getH2F("hi_theta_phi_pos"));
        canvasTabbed.getCanvas("CVT Positive Tracks").cd(7);
        canvasTabbed.getCanvas("CVT Positive Tracks").draw(dataGroups.getItem(6).getH2F("hi_chi2_vz_pos"));

        canvasTabbed.getCanvas("CVT Resolution").cd(0);
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getH1F("hi_dp_pos"));
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getF1D("f1_dp_pos"),"same");
        canvasTabbed.getCanvas("CVT Resolution").cd(1);
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getH1F("hi_dtheta_pos"));
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getF1D("f1_dtheta_pos"),"same");
        canvasTabbed.getCanvas("CVT Resolution").cd(2);
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getH1F("hi_dphi_pos"));
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getF1D("f1_dphi_pos"),"same");
        canvasTabbed.getCanvas("CVT Resolution").cd(3);
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getH1F("hi_dvz_pos"));
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getF1D("f1_dvz_pos"),"same");
        canvasTabbed.getCanvas("CVT Resolution").cd(4);
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getH1F("hi_dp_neg"));
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getF1D("f1_dp_neg"),"same");
        canvasTabbed.getCanvas("CVT Resolution").cd(5);
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getH1F("hi_dtheta_neg"));
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getF1D("f1_dtheta_neg"),"same");
        canvasTabbed.getCanvas("CVT Resolution").cd(6);
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getH1F("hi_dphi_neg"));
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getF1D("f1_dphi_neg"),"same");
        canvasTabbed.getCanvas("CVT Resolution").cd(7);
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getH1F("hi_dvz_neg"));
        canvasTabbed.getCanvas("CVT Resolution").draw(dataGroups.getItem(7).getF1D("f1_dvz_neg"),"same");

        //CVT Vertex 
        canvasTabbed.getCanvas("CVT Vertex").cd(0);
        canvasTabbed.getCanvas("CVT Vertex").draw(dataGroups.getItem(6).getH1F("hi_vz_pos"));
        canvasTabbed.getCanvas("CVT Vertex").draw(dataGroups.getItem(6).getF1D("f1_vz_pos"),"same");
        canvasTabbed.getCanvas("CVT Vertex").cd(1);
        canvasTabbed.getCanvas("CVT Vertex").draw(dataGroups.getItem(8).getH2F("hi_vz_vs_theta_pos"));
        canvasTabbed.getCanvas("CVT Vertex").cd(2);
        canvasTabbed.getCanvas("CVT Vertex").getPad(2).getAxisZ().setLog(true);
        canvasTabbed.getCanvas("CVT Vertex").draw(dataGroups.getItem(8).getH2F("hi_vxy_pos"));
        canvasTabbed.getCanvas("CVT Vertex").cd(3);
        canvasTabbed.getCanvas("CVT Vertex").draw(dataGroups.getItem(8).getH2F("hi_vz_vs_phi_pos"));
        canvasTabbed.getCanvas("CVT Vertex").cd(4);
        canvasTabbed.getCanvas("CVT Vertex").draw(dataGroups.getItem(5).getH1F("hi_vz_neg"));
        canvasTabbed.getCanvas("CVT Vertex").draw(dataGroups.getItem(5).getF1D("f1_vz_neg"),"same");
        canvasTabbed.getCanvas("CVT Vertex").cd(5);
        canvasTabbed.getCanvas("CVT Vertex").draw(dataGroups.getItem(8).getH2F("hi_vz_vs_theta_neg"));
        canvasTabbed.getCanvas("CVT Vertex").cd(6);
        canvasTabbed.getCanvas("CVT Vertex").getPad(6).getAxisZ().setLog(true);
        canvasTabbed.getCanvas("CVT Vertex").draw(dataGroups.getItem(8).getH2F("hi_vxy_neg"));
        canvasTabbed.getCanvas("CVT Vertex").cd(7);
        canvasTabbed.getCanvas("CVT Vertex").draw(dataGroups.getItem(8).getH2F("hi_vz_vs_phi_neg"));

        for(int sector=1; sector<=6; sector++) {
            canvasTabbed.getCanvas("TBT Positive Tracks by Sector").cd(sector-1);
            canvasTabbed.getCanvas("TBT Positive Tracks by Sector").draw(dataGroups.getItem(9).getH1F("hi_vz_pos_sec_" + sector));
            canvasTabbed.getCanvas("TBT Positive Tracks by Sector").draw(dataGroups.getItem(9).getH1F("hi_vz_pos_sec_" + sector + "_cut"),"same");
            canvasTabbed.getCanvas("TBT Positive Tracks by Sector").draw(dataGroups.getItem(9).getF1D("f1_vz_pos_sec_" + sector),"same");
            canvasTabbed.getCanvas("TBT Negative Tracks by Sector").cd(sector-1);
            canvasTabbed.getCanvas("TBT Negative Tracks by Sector").draw(dataGroups.getItem(10).getH1F("hi_vz_neg_sec_" + sector));
            canvasTabbed.getCanvas("TBT Negative Tracks by Sector").draw(dataGroups.getItem(10).getH1F("hi_vz_neg_sec_" + sector + "_cut"),"same");
            canvasTabbed.getCanvas("TBT Negative Tracks by Sector").draw(dataGroups.getItem(10).getF1D("f1_vz_neg_sec_" + sector),"same");
        }

        canvasTabbed.getCanvas("TBT-CVT Vertex Comparison").cd(0);
        canvasTabbed.getCanvas("TBT-CVT Vertex Comparison").draw(dataGroups.getItem(11).getH2F("vertex_CVT_TBT_pos"));
        canvasTabbed.getCanvas("TBT-CVT Vertex Comparison").cd(1);
        canvasTabbed.getCanvas("TBT-CVT Vertex Comparison").draw(dataGroups.getItem(11).getH1F("vertex_CVT_TBT_diff_pos"),"same");
        canvasTabbed.getCanvas("TBT-CVT Vertex Comparison").cd(2);
        canvasTabbed.getCanvas("TBT-CVT Vertex Comparison").draw(dataGroups.getItem(11).getH2F("vertex_CVT_TBT_neg"));
        canvasTabbed.getCanvas("TBT-CVT Vertex Comparison").cd(3);
        canvasTabbed.getCanvas("TBT-CVT Vertex Comparison").draw(dataGroups.getItem(11).getH1F("vertex_CVT_TBT_diff_neg"),"same");

        
        canvasTabbed.getCanvas("TBT Negative Tracks").update();
        canvasTabbed.getCanvas("TBT Positive Tracks").update();
        canvasTabbed.getCanvas("TBT Resolution").update();
        canvasTabbed.getCanvas("TBT Vertex").update();
        canvasTabbed.getCanvas("CVT Negative Tracks").update();
        canvasTabbed.getCanvas("CVT Positive Tracks").update();
        canvasTabbed.getCanvas("CVT Vertex").update();
        canvasTabbed.getCanvas("TBT Positive Tracks by Sector").update();
        canvasTabbed.getCanvas("TBT Negative Tracks by Sector").update();
	canvasTabbed.getCanvas("TBT-CVT Vertex Comparison").update();

    }
    
    private void analyze() {
        this.fitVertex(dataGroups.getItem(1).getH1F("hi_vz_neg_cut"), dataGroups.getItem(1).getF1D("f1_vz_neg"));
        this.fitVertex(dataGroups.getItem(2).getH1F("hi_vz_pos_cut"), dataGroups.getItem(2).getF1D("f1_vz_pos"));
        this.fitVertex(dataGroups.getItem(5).getH1F("hi_vz_neg"), dataGroups.getItem(5).getF1D("f1_vz_neg"));
        this.fitVertex(dataGroups.getItem(6).getH1F("hi_vz_pos"), dataGroups.getItem(6).getF1D("f1_vz_pos"));
        for(int sector=1; sector<=6; sector++) {
            this.fitVertex(dataGroups.getItem(10).getH1F("hi_vz_neg_sec_" + sector + "_cut"), dataGroups.getItem(10).getF1D("f1_vz_neg_sec_" + sector));
            this.fitVertex(dataGroups.getItem(9).getH1F("hi_vz_pos_sec_" + sector + "_cut"), dataGroups.getItem(9).getF1D("f1_vz_pos_sec_" + sector));
        }


        // fitting MC comparisons
        this.fitMC(dataGroups.getItem(3).getH1F("hi_dp_pos"),     dataGroups.getItem(3).getF1D("f1_dp_pos"));
        this.fitMC(dataGroups.getItem(3).getH1F("hi_dtheta_pos"), dataGroups.getItem(3).getF1D("f1_dtheta_pos"));
        this.fitMC(dataGroups.getItem(3).getH1F("hi_dphi_pos"),   dataGroups.getItem(3).getF1D("f1_dphi_pos"));
        this.fitMC(dataGroups.getItem(3).getH1F("hi_dvz_pos"),    dataGroups.getItem(3).getF1D("f1_dvz_pos"));     
        this.fitMC(dataGroups.getItem(3).getH1F("hi_dp_neg"),     dataGroups.getItem(3).getF1D("f1_dp_neg"));
        this.fitMC(dataGroups.getItem(3).getH1F("hi_dtheta_neg"), dataGroups.getItem(3).getF1D("f1_dtheta_neg"));
        this.fitMC(dataGroups.getItem(3).getH1F("hi_dphi_neg"),   dataGroups.getItem(3).getF1D("f1_dphi_neg"));
        this.fitMC(dataGroups.getItem(3).getH1F("hi_dvz_neg"),    dataGroups.getItem(3).getF1D("f1_dvz_neg"));     
        this.fitMC(dataGroups.getItem(7).getH1F("hi_dp_pos"),     dataGroups.getItem(7).getF1D("f1_dp_pos"));
        this.fitMC(dataGroups.getItem(7).getH1F("hi_dtheta_pos"), dataGroups.getItem(7).getF1D("f1_dtheta_pos"));
        this.fitMC(dataGroups.getItem(7).getH1F("hi_dphi_pos"),   dataGroups.getItem(7).getF1D("f1_dphi_pos"));
        this.fitMC(dataGroups.getItem(7).getH1F("hi_dvz_pos"),    dataGroups.getItem(7).getF1D("f1_dvz_pos"));     
        this.fitMC(dataGroups.getItem(7).getH1F("hi_dp_neg"),     dataGroups.getItem(7).getF1D("f1_dp_neg"));
        this.fitMC(dataGroups.getItem(7).getH1F("hi_dtheta_neg"), dataGroups.getItem(7).getF1D("f1_dtheta_neg"));
        this.fitMC(dataGroups.getItem(7).getH1F("hi_dphi_neg"),   dataGroups.getItem(7).getF1D("f1_dphi_neg"));
        this.fitMC(dataGroups.getItem(7).getH1F("hi_dvz_neg"),    dataGroups.getItem(7).getF1D("f1_dvz_neg"));     
        this.fitMCSlice(dataGroups.getItem(3).getH2F("hi_dp_p_neg"),dataGroups.getItem(3).getGraph("gr_dp_p_neg"));
        this.fitMCSlice(dataGroups.getItem(3).getH2F("hi_dp_theta_neg"),dataGroups.getItem(3).getGraph("gr_dp_theta_neg"));
        this.fitMCSlice(dataGroups.getItem(3).getH2F("hi_dp_phi_neg"),dataGroups.getItem(3).getGraph("gr_dp_phi_neg"));
    }

    private void fitVertex(H1F hivz, F1D f1vz) {
        double mean  = hivz.getDataX(hivz.getMaximumBin());
        double amp   = hivz.getBinContent(hivz.getMaximumBin());
        double sigma = 1.;
        if(hivz.getEntries()>500) { // first fits 
            sigma = Math.abs(f1vz.getParameter(2));       
        }
        f1vz.setParameter(0, amp);
        f1vz.setParameter(1, mean);
        f1vz.setParameter(2, sigma);
        f1vz.setRange(mean-2.*sigma,mean+2.*sigma);
        DataFitter.fit(f1vz, hivz, "Q"); //No options uses error for sigma 
        hivz.setFunction(null);
    }


    private void fitMC(H1F himc, F1D f1mc) {
        double mean  = himc.getDataX(himc.getMaximumBin());
        double amp   = himc.getBinContent(himc.getMaximumBin());
        double sigma = himc.getRMS()/2;
        f1mc.setParameter(0, amp);
        f1mc.setParameter(1, mean);
        f1mc.setParameter(2, sigma);
        f1mc.setRange(mean-2.*sigma,mean+2.*sigma);
        DataFitter.fit(f1mc, himc, "Q"); //No options uses error for sigma 
        sigma = Math.abs(f1mc.getParameter(2));  
        f1mc.setRange(mean-2.*sigma,mean+2.*sigma);
        DataFitter.fit(f1mc, himc, "Q"); //No options uses error for sigma 
        himc.setFunction(null);
    }
    
    private void fitMCSlice(H2F himc, GraphErrors grmc) {
        grmc.reset();
        ArrayList<H1F> hslice = himc.getSlicesX();
        for(int i=0; i<hslice.size(); i++) {
            double  x = himc.getXAxis().getBinCenter(i);
            double ex = 0;
            double  y = hslice.get(i).getRMS();
            double ey = 0;
            double mean  = hslice.get(i).getDataX(hslice.get(i).getMaximumBin());
            double amp   = hslice.get(i).getBinContent(hslice.get(i).getMaximumBin());
            double sigma = hslice.get(i).getRMS()/2;
            F1D f1 = new F1D("f1slice","[amp]*gaus(x,[mean],[sigma])", hslice.get(i).getDataX(0), hslice.get(i).getDataX(hslice.get(i).getDataSize(1)-1));
            f1.setParameter(0, amp);
            f1.setParameter(1, mean);
            f1.setParameter(2, sigma);
            f1.setRange(mean-2.*sigma,mean+2.*sigma);
            DataFitter.fit(f1, hslice.get(i), "Q"); //No options uses error for sigma 
            if(amp>50) grmc.addPoint(x, f1.getParameter(2), ex, f1.parameter(2).error());
        }

    }
    
    public void setAnalysisTabNames(String... names) {
        for(String name : names) {
            canvasTabNames.add(name);
        }
        canvasTabbed = new EmbeddedCanvasTabbed(names);
    }
    
    public void printCanvas(String dir, String name) {
        // print canvas to files
        for(int tab=0; tab<canvasTabNames.size(); tab++) {
            String fileName = dir + "/" + this.analysisName + "_" + name + "." + tab + ".png";
            System.out.println(fileName);
            canvasTabbed.getCanvas(canvasTabNames.get(tab)).save(fileName);
        }
    }
    
    public boolean testMCpart(Particle mc, Particle rec) {
        if(Math.abs(mc.px()-rec.px())<0.5 &&
           Math.abs(mc.py()-rec.py())<0.5 &&
           Math.abs(mc.pz()-rec.pz())<0.5) return true;
        else return false;
    }
}
