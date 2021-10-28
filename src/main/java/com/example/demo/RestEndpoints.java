package com.example.demo;

import com.sforce.soap.metadata.*;
import com.sforce.ws.ConnectionException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
public class RestEndpoints {
//    @GetMapping("/")
//    public String home(){
//        return "Welcome to salesforce api handler";
//    }


    static class IPRanges{
        public String start;
        public String end;
        public String description;

        public IpRange toIpRange(){
            IpRange ourRange = new IpRange();
            ourRange.setStart(start);
            ourRange.setEnd(end);
            ourRange.setDescription(description);
            return ourRange;
        }

        public ProfileLoginIpRange toProfileLogin(){
            ProfileLoginIpRange range = new ProfileLoginIpRange();

            range.setStartAddress(start);
            range.setEndAddress(end);
            range.setDescription(description);
            return range;

        }
    }

    static class NetworkIPParams{
        public List<IPRanges> ipRanges;

        boolean check(){
            return ipRanges != null;
        }

    }


    @PostMapping("/ipranges")
    public String ipRanges(@RequestBody NetworkIPParams params) throws ConnectionException {
        if (!params.check()){
            return "Invalid Request";
        }

        ReadResult readResult = SpringSfApiApplication.connection.readMetadata("SecuritySettings", new String[]{"Security.settings"});

        Metadata[] records = readResult.getRecords();

        for (Metadata obj : records){
            SecuritySettings securitySettings = (SecuritySettings) obj;

            IpRange[] ipRanges = securitySettings.getNetworkAccess().getIpRanges();

            List<IpRange> ipRangeList = new ArrayList<>(Arrays.asList(ipRanges));

            for (IPRanges ourRange : params.ipRanges){
                ipRangeList.add(ourRange.toIpRange());
            }

            System.out.println(ipRangeList.toString());

            securitySettings.getNetworkAccess().setIpRanges(ipRangeList.toArray(IpRange[]::new));
        }

        SaveResult saveResult = SpringSfApiApplication.connection.updateMetadata(readResult.getRecords())[0];

        if (!saveResult.isSuccess()){
            System.out.println(saveResult.getErrors()[0].getMessage());
        }
        return "Done";
    }

    static class ProfileIpParam{
        public String name;
        public List<IPRanges> ipRanges;

        boolean check(){
            return name != null && !name.equals("") && ipRanges != null;

        }
    }

//     Not Working. Unable to update anything in Profile.
    @PostMapping("/profileip")
    public String profileIp(@RequestBody ProfileIpParam params) throws ConnectionException {

        if (!params.check()){
            return "Invalid Request";
        }


        ReadResult readResult = SpringSfApiApplication.connection.readMetadata("Profile", new String[]{params.name});

        Metadata[] records = readResult.getRecords();

        if (records.length == 0){
            return "Something went wrong";
        }

        for (Metadata record: records){
            System.out.println("Fullname " + record.getFullName());
            Profile profile = (Profile) record;
            System.out.println("profile " + profile.isCustom() +" " + profile.getFullName());

//            Checking if we can edit description or not.
//            profile.setDescription("Edited");

//            Code for adding IP ranges below.

            ProfileLoginIpRange[] profileLoginIpRanges = profile.getLoginIpRanges();
            List<ProfileLoginIpRange> ranges = new ArrayList<>(Arrays.asList(profileLoginIpRanges));

            for (IPRanges ipRanges : params.ipRanges){
                ranges.add(ipRanges.toProfileLogin());
            }

            profile.setLoginIpRanges(ranges.toArray(ProfileLoginIpRange[]::new));
        }

//        Save result throws error.
        SaveResult saveResult = SpringSfApiApplication.connection.updateMetadata(readResult.getRecords())[0];

        if (!saveResult.isSuccess()){

//            You can't edit tab settings for (AccountInsight), as it's not a valid tab.
//            (AccountInsight) changes depending on which profile we are editing.
            System.out.println("Error");
            System.out.println(saveResult.getErrors()[0].getMessage());
            return "ERROR";
        }

        return "DONE";
    }

    static class ProfileNameParam{
        public String name;
    }
    @DeleteMapping("/deleteprofile")
    public String deleteProfile(@RequestBody ProfileNameParam params) throws ConnectionException {

        DeleteResult[] deleteResults = SpringSfApiApplication.connection.deleteMetadata("Profile", new String[]{params.name});

        if (deleteResults.length == 0){
            return "Something went wrong";
        }

        if (!deleteResults[0].isSuccess()){
            System.out.println("Error");
            System.out.println(deleteResults[0].getErrors()[0].getMessage());
        }

        return "DONE";
    }

    static class ValidationRuleParams{
        public String name;
        public boolean active;

        public boolean check(){
            return name != null && !name.equals("");
        }
    }

    @PostMapping("/updatevalidation")
    public String updateValidationRules(@RequestBody ValidationRuleParams params) throws ConnectionException {
        if (!params.check()){
            return "Bad request";
        }

        ReadResult readResult = SpringSfApiApplication.connection.readMetadata("ValidationRule", new String[]{params.name});

        Metadata[] records = readResult.getRecords();

        if (records.length == 0){
            return "Invalid request";
        }

        for (Metadata record : records){
            ValidationRule validationRule = (ValidationRule) record;
            System.out.println("Validation rule is: " + validationRule.getFullName());
            validationRule.setActive(params.active);
        }

        SaveResult saveResult = SpringSfApiApplication.connection.updateMetadata(readResult.getRecords())[0];

        if (!saveResult.isSuccess()){
            System.out.println("error");
            System.out.println(saveResult.getErrors()[0].getMessage());
            return "ERROR";
        }
        return "DONE";
    }

}