package preprocessing;//class들의 집합

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Spectrum {
	
	String title;
	double rtinseconds;
	Peak pepmass;
	int charge;
	
	ArrayList<Peak> peak_list;
	
	public Spectrum()
	{
		this.peak_list = new ArrayList<Peak>();
	}

	@Override
	public String toString()
	{
		String str = "";
		
		str += "BEGIN IONS" + '\n';
		str += "TITLE=" + this.title + '\n';
		str += "RTINSECONDS=" + String.valueOf(this.rtinseconds) + '\n';
		str += "PEPMASS=" + pepmass.toString() + '\n';
		str += "CHARGE=" + String.valueOf(this.charge) + '+' + '\n';
		
		for (Peak peak: peak_list)
			str += peak.toString() + '\n';
		
		str += "END IONS";
		
		return str;
	}
	
	public void load(ArrayList<String> lines)
	{
		for (String line: lines)
		{
			if (line.contains("TITLE")) {
				int index = line.indexOf('=');
				this.title = line.substring(index + 1).trim();
			} else if (line.contains("RTINSECONDS")) {
				int index = line.indexOf('=');
				this.rtinseconds = Double.parseDouble(line.substring(index + 1));
			} else if (line.contains("PEPMASS")) {
				int index = line.indexOf('=');
				String[] split_line = line.substring(index + 1).split(" ");
				double mz = Double.parseDouble(split_line[0]);
				double intensity = Double.parseDouble(split_line[1]);
				this.pepmass = new Peak(mz, intensity);
			} else if (line.contains("CHARGE")) {
				int index = line.indexOf('=');
				this.charge = Integer.parseInt(line.substring(index + 1).split("\\+")[0]);
			} else {
				String[] split_line = line.split(" ");
				double mz = Double.parseDouble(split_line[0]);
				double intensity = Double.parseDouble(split_line[1]);
				this.peak_list.add(new Peak(mz, intensity));
			}

		}
	}
	
	public int get_max_intensity_index(ArrayList<Peak> peak_list)
	{
		int max_index = 0;
		double max_intensity = 0;
		
		for (int i = 0; i < peak_list.size(); i++)
		{
			Peak peak = peak_list.get(i);
			if (max_intensity < peak.intensity) {
				max_index = i;
				max_intensity = peak.intensity;
			}
		}
		return max_index;
	}
	
	public double calculate_tolerance(double ppm, double mz)
	{
		return ppm * mz * Math.pow(10, -6);
	}
	
	public Peak binning(ArrayList<Peak> peak_list, int index, double ppm)
	{

        int left_index = index;//left와 right_index는 binning하기위한 tolerance 범위를 체크하기 위함이다.
        int right_index = index;//

        Peak cur_peak = peak_list.get(index);//index번째 peak을 받기 위함.

		double tolerance = this.calculate_tolerance(ppm, cur_peak.mz);//tolerance 계산할 때는 mz값이 필요하다. -> 잘 이용하면 할 수 있을듯
		
		for (left_index = index - 1; left_index >= 0; left_index--)//tolerance 범위를 넘어가면 for문 탈출 -> tolerance를 try_catch문안에 넣어보면 어떨까?
		{
			Peak left_peak = peak_list.get(left_index);
			if (cur_peak.mz - left_peak.mz > tolerance)
				break;
		}
		left_index += 1;

        for (right_index = index + 1; right_index < peak_list.size(); right_index++)
		{
			Peak right_peak = peak_list.get(right_index);
			if (right_peak.mz - cur_peak.mz > tolerance)
				break;
		}
		right_index -= 1;

		double weighted_mz = 0;
		double total_intensity = 0;
		
		for (int i = left_index; i <= right_index; i++) {//total_peak은 intensity값들의 합 과 mz값들의 합을 더하여
			Peak temp_peak = peak_list.get(i);//temp_peak은 left_index와 right_index들의 범위안의 intensity와 mz값들을 더한 peak이다.
			weighted_mz += temp_peak.mz * temp_peak.intensity;
			total_intensity += temp_peak.intensity;
		}
		weighted_mz /= total_intensity;
		
		for (int i = 0; i <= right_index - left_index; i++) {
			peak_list.remove(left_index);
		}
		
		return new Peak(weighted_mz, total_intensity);
	}


	/*
	* Time Cost가 다르지 않고 Top N개로 뽑아도 time cost가 별반 다르지 않아 신뢰성에서
	* 문제가 제기된다. 앞으로의 프로젝트의 방향을 틀도록 해야한다.
	*
	*
	*
	* */
    public Peak binning_using_mz(ArrayList<Peak> peak_list, double mz, double ppm) {

        int num_index = 0;
        Peak isotope_peak = new Peak(0, 0);

        for (int i = 0; i < peak_list.size(); i++) {
            if (mz == peak_list.get(i).mz) {
                isotope_peak.mz = mz;
                isotope_peak.intensity = peak_list.get(i).intensity;
                num_index = i;
                break;
            }
        }

        isotope_peak = this.binning(peak_list, num_index, ppm);//isotope를 적용한 peak을 binning 한 값

        return isotope_peak;

    }
    /*
	public Peak isotope(ArrayList<Peak> peak_list, int index, double ppm) {

	    Peak cur_peak = this.binning(peak_list, index, ppm);//cur_peak에 isotope에 맞는 mz에 맞게 binning한 peak들을 저장 -> mz로 받는게 좋을듯
	    Peak[] isotope_peak_list = new Peak[2*this.charge];//isotope_peak_list에 (charge = 1  * 2)


	    for (int i = -this.charge; i != this.charge + 1; i++) {//-charge ~ +charge 전까지 루프
	        int mz_difference = 1/this.charge; //mz_difference = 1/charge = mass unit
	        Peak isotope_peak = this.binning(peak_list, cur_peak.mz + i*mz_difference, ppm);//binning index부분을 이용하지 않고 mz를 이용, M + 1 = cur_peak.mz + i * mz_difference
	        isotope_peak_list[i + this.charge] = isotope_peak;// isotope_peak_list[0 ~ charge * 2] = isotope를 적용한 peak들을 저장한다.
        }

        for (Peak isotope_peak: isotope_peak_list) {//
            cur_peak.mz += isotope_peak.mz; //cur_peak은 isotope를 적용한 peak들의 mz 값들의 합?
            cur_peak.intensity += isotope_peak.intensity;//cur_peak은 isotope를 적용한 peak들의 intensity들을 합친값
        }

        peak_list.remove(cur_peak);//peak_list의 cur_peak(binning한 peak)을 지웁니다.
        for (Peak isotope_peak: isotope_peak_list) { //isotope_peak_list안에 isotope를 적용한 peak들을 지웁니다.
            peak_list.remove(isotope_peak);
        }

        return cur_peak;//binning한 peak을 출력합니다.
    }*/

    public Peak isotope(ArrayList<Peak> peak_list, double ppm, int index) {

        Peak binning_peak = this.binning(peak_list, index, ppm);//binning을 먼저하고
        Peak[] isotope_peak_list = new Peak[2*this.charge+1];
        //System.out.println(isotope_peak_list);

        for (int i = -this.charge; i != this.charge + 1; i++) {//isotope의 각 peak들을 적용한다.
            float mz_difference = 1/this.charge;//mz_difference = 1/charge = mass unit
            Peak isotope_peak = this.binning_using_mz(peak_list, binning_peak.mz + i*mz_difference, ppm);
            isotope_peak_list[i + this.charge] = isotope_peak;
        }


        for (Peak iso_peak: isotope_peak_list) {//isotope peak들을 binning 한다.
            binning_peak.mz += iso_peak.mz * iso_peak.intensity;
            binning_peak.intensity += iso_peak.intensity;
        }

        double weighted_mz = binning_peak.mz;//isotope을 할때 binning을 하고 mz값을 어떻게 만들까?
        double total_intensity = binning_peak.intensity;

        weighted_mz /= total_intensity;

        peak_list.remove(binning_peak);
        for (Peak isotope_peak: isotope_peak_list){
            peak_list.remove(isotope_peak);
        }

        return new Peak(weighted_mz, total_intensity);
    }

	public void preprocessing(String HOW, double ppm, int N)//전처리 과정, N과 ppm을 설정한다 -> HOW = --binning을 할지, --isotope을 할지 결정한다.
	{
		ArrayList<Peak> copy_peak_list = this.peak_list;
		this.peak_list = new ArrayList<Peak>();
		
		int cnt = 0;
		while (true)
		{
			if (cnt >= N) break;
			else if (copy_peak_list.size() == 0) break;
			
			int max_index = this.get_max_intensity_index(copy_peak_list);
			Peak peak = this.binning(copy_peak_list, max_index, ppm);
			//Peak peak = this.isotope(copy_peak_list, ppm, max_index);

			this.peak_list.add(peak);
			cnt++;
		}
		
		this.peak_list.sort(null);
	}
	
	public static void main(String[] args) throws IOException, IndexOutOfBoundsException {
		String file_in = "mgf/20100609_Velos1_TaGe_SA_293_2.mgf";
		String file_out = "mgf/20100609_Velos1_TaGe_SA_293_2.binning_10.mgf";
        //String file_out = "mgf/20100609_Velos1_TaGe_SA_293_2.isotope_100.mgf";

        double ppm = 20;
		int N = 100;
		
		ArrayList<Spectrum> spectrum_list = new ArrayList<Spectrum>(); //Arraylist.spectrum_list안에 정보들을 저장한다.
		
		BufferedReader in = new BufferedReader(new FileReader(file_in));
		
		int cnt = 0;
		ArrayList<String> lines = null;
		while (true)
		{
//			if (cnt >= 1) break;
			
			String line = in.readLine();
			if (line == null) break;
			line = line.trim();
			
			if (line.equals("BEGIN IONS")) {
				lines = new ArrayList<String>();
				continue;
			} else if (line.equals("END IONS")) {
				Spectrum spectrum = new Spectrum();
				spectrum.load(lines);
				spectrum_list.add(spectrum);
				cnt++;
				continue;
			}
			lines.add(line);
		}
		
		in.close();
		
		BufferedWriter out = new BufferedWriter(new FileWriter(file_out));
		
		for (Spectrum spectrum: spectrum_list)
		{
			spectrum.preprocessing("--binning", ppm, N);
			//spectrum.preprocessing("--isotope", ppm, N);
            out.write(spectrum.toString());
			out.write('\n');
		}
		
		out.close();
	}
}

class Peak implements Comparable<Peak> {
	double mz;
	double intensity;
	
	Peak(double mz, double intensity)
	{
		this.mz = mz;
		this.intensity = intensity;
	}
	
	@Override
	public String toString()
	{
		return String.valueOf(this.mz) + ' ' + String.valueOf(this.intensity); 
	}

	@Override
	public int compareTo(Peak peak) {
		if (this.mz < peak.mz) return -1;
		else if (this.mz == peak.mz) return 0;
		else return 1;
	}
}