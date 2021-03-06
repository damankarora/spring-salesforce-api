package com.example.demo;

import com.sforce.soap.metadata.*;
import com.sforce.ws.ConnectionException;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
public class RestEndpoints {
    @GetMapping("/contact")
    public String home(){
        return "Welcome to salesforce api handler contact page";
    }


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

    static class RuleParams{
        public String name;
        public boolean active;

        public boolean check(){
            return name != null && !name.equals("");
        }
    }

    @PostMapping("/updatevalidation")
    public String updateValidationRules(@RequestBody RuleParams params) throws ConnectionException {
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



    @PostMapping("/updateworkflowrule")
    public String updateWorkflowRule(@RequestBody RuleParams params) throws ConnectionException {
        if (!params.check()){
            return "Bad request";
        }

        ReadResult readResult = SpringSfApiApplication.connection.readMetadata("WorkflowRule", new String[]{params.name});

        Metadata[] records = readResult.getRecords();

        if(records.length == 0 || records[0] == null){
            return "Invalid name! Workflow rule not found";
        }

        for (Metadata rec : records){
            WorkflowRule workflowRule = (WorkflowRule) rec;

            System.out.println("Found: " + workflowRule.getFullName());

            workflowRule.setActive(params.active);
        }

        SaveResult saveResult = SpringSfApiApplication.connection.updateMetadata(readResult.getRecords())[0];

        if (!saveResult.isSuccess()){
            System.out.println("Error");
            System.out.println(saveResult.getErrors()[0].getMessage());
            return "ERROR";
        }

        return "DONE";

    }


    static class AlertRecipient{
        public String recipient;
        public String type;

        public WorkflowEmailRecipient toWorkflowEmailRecipient(){
            WorkflowEmailRecipient workflowEmailRecipient = new WorkflowEmailRecipient();
            workflowEmailRecipient.setRecipient(recipient);
            if (type.toLowerCase().equals("user")){
                workflowEmailRecipient.setType(ActionEmailRecipientTypes.user);
            }else{
                workflowEmailRecipient.setType(ActionEmailRecipientTypes.owner);
            }
            return workflowEmailRecipient;
        }
    }

    static class WorkflowAlertParams{
        public String name;
        public List<AlertRecipient> recipients;

        public boolean check(){
            return name != null && recipients != null;
        }
    }

    @PostMapping("/workflowalert")
    public String workflowAlert(@RequestBody WorkflowAlertParams params) throws ConnectionException {
        if (!params.check()){
            return "Bad request";
        }

        ReadResult readResult = SpringSfApiApplication.connection.readMetadata("WorkflowAlert", new String[]{params.name});
        Metadata[] records = readResult.getRecords();

        if (records.length == 0){
            return "ERROR";
        }

        for (Metadata record : records){

            WorkflowAlert workflowAlert = (WorkflowAlert) record;
            System.out.println("Found: " + workflowAlert.getFullName());

            WorkflowEmailRecipient[] prevRecipients = workflowAlert.getRecipients();
            List<WorkflowEmailRecipient> emailRecipientList = new ArrayList<>(Arrays.asList(prevRecipients));

            for (AlertRecipient recipient : params.recipients){
                emailRecipientList.add(recipient.toWorkflowEmailRecipient());
            }

            workflowAlert.setRecipients(emailRecipientList.toArray(WorkflowEmailRecipient[]::new));
        }

        SaveResult saveResult = SpringSfApiApplication.connection.updateMetadata(readResult.getRecords())[0];

        if (!saveResult.isSuccess()){
            System.out.println("Error");
            System.out.println(saveResult.getErrors()[0].getMessage());
            return "ERROR";
        }
        return "DONE";
    }

    static class AdditionalEmailParams{
        public String name;

        public boolean check(){
            return name != null && !name.isEmpty();

        }
    }

    @DeleteMapping("/removeadditionals")
    public String removeAdditionalEmails(@RequestBody AdditionalEmailParams params) throws ConnectionException {
        if (!params.check()){
            return "ERROR";
        }

        ReadResult readResult = SpringSfApiApplication.connection.readMetadata("WorkflowAlert", new String[]{params.name});
        Metadata[] records = readResult.getRecords();
        if (records.length == 0 || records[0] == null){
            return "ERROR";
        }
        for (Metadata record : records){
           WorkflowAlert workflowAlert = (WorkflowAlert) record;
            System.out.println("FOUND: " + workflowAlert.getFullName());
            workflowAlert.setCcEmails(null);
        }

        SaveResult saveResult = SpringSfApiApplication.connection.updateMetadata(readResult.getRecords())[0];

        if (!saveResult.isSuccess()){
            System.out.println("ERROR");
            System.out.println(saveResult.getErrors()[0].getMessage());
            return "ERROR";
        }

        return "DONE";

    }

    //    Creating a new Permission set.
//    Didn't work...getErrors().length is also zero.

//    static class PermissionSetUserParams{
//        public String name;
//        public boolean enabled;
//
//        public PermissionSetUserPermission toPermissionSetUserPermission(){
//            PermissionSetUserPermission permset = new PermissionSetUserPermission();
//            permset.setName(name);
//            permset.setEnabled(enabled);
//            return permset;
//        }
//    }
//
//    static class PermissionSetParams{
//        public String name;
//        public List<PermissionSetUserParams> perms;
//
//        public boolean check(){
//            return name != null && !name.isEmpty() && perms != null;
//        }
//    }
//
//    @PostMapping("/permissionset")
//    public String createPermissionSet(@RequestBody PermissionSetParams params) throws ConnectionException {
//
//        if (!params.check()){
//
//            return "Error";
//        }
//
//        PermissionSet permissionSet = new PermissionSet();
//        permissionSet.setLabel(params.name);
//
//        List<PermissionSetUserPermission> userPermissions = new ArrayList<>();
//
//        for (PermissionSetUserParams perm : params.perms){
//            userPermissions.add(perm.toPermissionSetUserPermission());
//        }
//
//        permissionSet.setUserPermissions(userPermissions.toArray(PermissionSetUserPermission[]::new));
//
//        SaveResult saveResult = SpringSfApiApplication.connection.createMetadata(new Metadata[]{permissionSet})[0];
//
//        if (!saveResult.isSuccess()){
//            System.out.println("ERROR");
//            System.out.println(saveResult.getErrors().length);
//            return "ERROR";
//        }
//        return "DONE";
//    }



}