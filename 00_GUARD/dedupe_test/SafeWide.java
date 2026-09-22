import java.util.*;
class SafeWide {
    static final Map<String,String> ALL = new HashMap<>();
    static final Map<String,List<String>> HEAVY = new HashMap<>();
    static {
        ALL.put("patients","id,address,age,altMobile,bill,branch,complaint,completeApprovedBy,completeRequestedBy,createdAt,createdBy,date,decision,diagnosis,discount,disease,doctorAdvice,doctorComplete,doctorFullNote,medicalHistory,mobile,name,occupation,patientId,photo,previousCost,previousResult,previousTreatment,queue,refBy,refDoctor,refDoctorMobile,refundRestoredBy,registeredBy,registrationDate,sex,sinceWhen,stage,timeType,treatmentDuration,updatedAt,visitDate");
        ALL.put("followups","id,address,age,branch,callCount,convertedPatientId,createdAt,createdBy,date,disease,history,lastCallDate,lastRemark,mobile,name,nextFollow,patientId,photo,refId,registrationDate,sex,stage,status,timeType,updatedAt,visitDate");
        ALL.put("payments","id,amount,branch,cashAmount,createdAt,createdBy,dailyEvents,date,editHistory,editedAt,editedBy,mobile,mode,name,onlineAmount,patientCode,patientId,payLabel,payType,paymentLabel,receivedBy,refundApprovalStatus,remarks,updatedAt");
        ALL.put("medical","id,branch,createdAt,createdBy,date,days,decision,details,diagnosis,doctorFullNote,mobile,name,nextFollow,patientId,photos,selected,type,updatedAt");
        ALL.put("doctor_visits","id,area,branch,callHistory,callStatus,createdAt,createdBy,date,lastCallDate,mobile,name,nextCallDate,referralDue,referralPaid,referralPayments,remarks,status,updatedAt");
        HEAVY.put("patients",Arrays.asList("photo","medicalHistory"));
        HEAVY.put("followups",Arrays.asList("photo","history"));
        HEAVY.put("medical",Arrays.asList("photos","details"));
        HEAVY.put("doctor_visits",Arrays.asList("callHistory","referralPayments"));
        HEAVY.put("payments",Arrays.asList("editHistory"));
    }
    static String forTable(String table,String wantedCols){
        String all = ALL.get(table); if(all==null) return null;
        List<String> heavy = HEAVY.get(table); if(heavy==null) return null;
        List<String> known = new ArrayList<>();
        for(String c: all.split(",")){ String n=c.trim(); if(!n.isEmpty()) known.add(n); }
        Set<String> knownSet = new HashSet<>(known);
        Set<String> wanted = new HashSet<>();
        if(wantedCols!=null) for(String w: wantedCols.split(",")){ w=w.trim(); if(!w.isEmpty()) wanted.add(w); }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for(String n: known){ if(heavy.contains(n) && !wanted.contains(n)) continue; out.add(n); }
        for(String w: wanted) if(!w.equals("*") && knownSet.contains(w)) out.add(w);
        return out.isEmpty()? null : String.join(",",out);
    }
}
