package com;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.axis.types.NonNegativeInteger;

import com.fedex.rate.stub.*;


/**
 * Sample code to call Rate Web Service with Axis
 * <p>
 * com.fedex.rate.stub is generated via WSDL2Java, like this:<br>
 * <pre>
 * java org.apache.axis.wsdl.WSDL2Java -w -p com.fedex.rate.stub http://www.fedex.com/...../RateService?wsdl
 * </pre>
 * <p>
 * This sample code has been tested with JDK 7 and Apache Axis 1.4
 */
public class RateWebServiceClient {
    //
    public static void main(String[] args) {
        // Build a RateRequest request object
        boolean getAllRatesFlag = true; // set to true to get the rates for different service types
        RateRequest request = new RateRequest();
        request.setClientDetail(createClientDetail());
        request.setWebAuthenticationDetail(createWebAuthenticationDetail());
        request.setReturnTransitAndCommit(true);
        //
        TransactionDetail transactionDetail = new TransactionDetail();
        transactionDetail.setCustomerTransactionId("java sample - Rate Request"); // The client will get the same value back in the response
        request.setTransactionDetail(transactionDetail);

        //
        VersionId versionId = new VersionId("crs", 26, 0, 0);

        request.setVersion(versionId);

        //
        RequestedShipment requestedShipment = new RequestedShipment();

        requestedShipment.setShipTimestamp(Calendar.getInstance(TimeZone.getTimeZone("PST")));
        requestedShipment.setDropoffType(DropoffType.REGULAR_PICKUP);
        if (!getAllRatesFlag) {
            requestedShipment.setServiceType("PRIORITY_OVERNIGHT");
            requestedShipment.setPackagingType("YOUR_PACKAGING");
        }


        Party shipper = new Party();
        Address shipperAddress = new Address(); // Origin information
        shipperAddress.setStreetLines(new String[]{"1 Shipper St"});
        shipperAddress.setCity("Memphis");
        shipperAddress.setStateOrProvinceCode("TN");
        shipperAddress.setPostalCode("38115");
        shipperAddress.setCountryCode("US");
        shipper.setAddress(shipperAddress);
        requestedShipment.setShipper(shipper);

        //
        Party recipient = new Party();
        Address recipientAddress = new Address(); // Destination information
        recipientAddress.setStreetLines(new String[]{"1 Recipient St"});
        recipientAddress.setCity("COLLIERVILLE");
        recipientAddress.setStateOrProvinceCode("TN");
        recipientAddress.setPostalCode("38017");
        recipientAddress.setCountryCode("US");
        recipient.setAddress(recipientAddress);
        requestedShipment.setRecipient(recipient);

        //
        Payment shippingChargesPayment = new Payment();
        shippingChargesPayment.setPaymentType(PaymentType.SENDER);
        requestedShipment.setShippingChargesPayment(shippingChargesPayment);

        RequestedPackageLineItem rp = new RequestedPackageLineItem();
        rp.setGroupPackageCount(new NonNegativeInteger("1"));
        rp.setWeight(new Weight(WeightUnits.LB, new BigDecimal(15.0)));
        //
        rp.setInsuredValue(new Money("USD", new BigDecimal("100.00")));
        //
        rp.setDimensions(new Dimensions(new NonNegativeInteger("1"), new NonNegativeInteger("1"), new NonNegativeInteger("1"), LinearUnits.IN));
        PackageSpecialServicesRequested pssr = new PackageSpecialServicesRequested();
        rp.setSpecialServicesRequested(pssr);
        requestedShipment.setRequestedPackageLineItems(new RequestedPackageLineItem[]{rp});


        requestedShipment.setPackageCount(new NonNegativeInteger("1"));
        request.setRequestedShipment(requestedShipment);

        //
        try {
            // Initialize the service
            RateServiceLocator service;
            RatePortType port;
            //
            service = new RateServiceLocator();
            updateEndPoint(service);
            port = service.getRateServicePort();
            // This is the call to the web service passing in a RateRequest and returning a RateReply
            RateReply reply = port.getRates(request); // Service call
            if (isResponseOk(reply.getHighestSeverity())) {
                writeServiceOutput(reply);
            }
            printNotifications(reply.getNotifications());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeServiceOutput(RateReply reply) {
        RateReplyDetail[] rrds = reply.getRateReplyDetails();
        for (int i = 0; i < rrds.length; i++) {
            RateReplyDetail rrd = rrds[i];
            print("\nService type", rrd.getServiceType());
            print("Packaging type", rrd.getPackagingType());
            print("Delivery DOW", rrd.getDeliveryDayOfWeek());
            if (rrd.getDeliveryDayOfWeek() != null) {
                Calendar deliveryTimestamp = rrd.getDeliveryTimestamp();
                deliveryTimestamp.setTimeZone(TimeZone.getTimeZone("PST"));
                int month = deliveryTimestamp.get(Calendar.MONTH) + 1;
                int date = deliveryTimestamp.get(Calendar.DAY_OF_MONTH);
                int year = deliveryTimestamp.get(Calendar.YEAR);
                String delDate = new String(month + "/" + date + "/" + year);
                print("Delivery date", delDate);
                print("Calendar DOW", rrd.getDeliveryTimestamp().get(Calendar.DAY_OF_WEEK));
            }

            RatedShipmentDetail[] rsds = rrd.getRatedShipmentDetails();
            for (int j = 0; j < rsds.length; j++) {
                print("RatedShipmentDetail " + j, "");
                RatedShipmentDetail rsd = rsds[j];
                ShipmentRateDetail srd = rsd.getShipmentRateDetail();
                print("  Rate type", srd.getRateType());
                printWeight("  Total Billing weight", srd.getTotalBillingWeight());
                printMoney("  Total surcharges", srd.getTotalSurcharges());
                printMoney("  Total net charge", srd.getTotalNetCharge());

                RatedPackageDetail[] rpds = rsd.getRatedPackages();
                if (rpds != null && rpds.length > 0) {
                    print("  RatedPackageDetails", "");
                    for (int k = 0; k < rpds.length; k++) {
                        print("  RatedPackageDetail " + i, "");
                        RatedPackageDetail rpd = rpds[k];
                        PackageRateDetail prd = rpd.getPackageRateDetail();
                        if (prd != null) {
                            printWeight("    Billing weight", prd.getBillingWeight());
                            printMoney("    Base charge", prd.getBaseCharge());
                            Surcharge[] surcharges = prd.getSurcharges();
                            if (surcharges != null && surcharges.length > 0) {
                                for (int m = 0; m < surcharges.length; m++) {
                                    Surcharge surcharge = surcharges[m];
                                    printMoney("    " + surcharge.getDescription() + " surcharge", surcharge.getAmount());
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("");
        }
    }

    private static ClientDetail createClientDetail() {
        ClientDetail clientDetail = new ClientDetail();
        String accountNumber = System.getProperty("accountNumber");
        String meterNumber = System.getProperty("meterNumber");

        //
        // See if the accountNumber and meterNumber properties are set,
        // if set use those values, otherwise default them to "XXX"
        //
        if (accountNumber == null) {
            accountNumber = "xxx"; // Replace "XXX" with clients account number
        }
        if (meterNumber == null) {
            meterNumber = "xxx"; // Replace "XXX" with clients meter number
        }
        clientDetail.setAccountNumber(accountNumber);
        clientDetail.setMeterNumber(meterNumber);
        return clientDetail;
    }

    private static WebAuthenticationDetail createWebAuthenticationDetail() {
        WebAuthenticationCredential userCredential = new WebAuthenticationCredential();
        String key = System.getProperty("key");
        String password = System.getProperty("password");
        //
        // See if the key and password properties are set,
        // if set use those values, otherwise default them to "XXX"
        //
        if (key == null) {
            key = "xxx"; // Replace "XXX" with clients key
        }
        if (password == null) {
            password = "xxx"; // Replace "XXX" with clients password
        }
        userCredential.setKey(key);
        userCredential.setPassword(password);

        WebAuthenticationCredential parentCredential = null;
        Boolean useParentCredential = false; //Set this value to true is using a parent credential
        if (useParentCredential) {

            String parentKey = System.getProperty("parentkey");
            String parentPassword = System.getProperty("parentpassword");
            //
            // See if the parentkey and parentpassword properties are set,
            // if set use those values, otherwise default them to "XXX"
            //
            if (parentKey == null) {
                parentKey = "XXX"; // Replace "XXX" with clients parent key
            }
            if (parentPassword == null) {
                parentPassword = "XXX"; // Replace "XXX" with clients parent password
            }
            parentCredential = new WebAuthenticationCredential();
            parentCredential.setKey(parentKey);
            parentCredential.setPassword(parentPassword);
        }
        return new WebAuthenticationDetail(parentCredential, userCredential);
    }

    private static void printNotifications(Notification[] notifications) {
        System.out.println("Notifications:");
        if (notifications == null || notifications.length == 0) {
            System.out.println("  No notifications returned");
        }
        for (int i = 0; i < notifications.length; i++) {
            Notification n = notifications[i];
            System.out.print("  Notification no. " + i + ": ");
            if (n == null) {
                System.out.println("null");
                continue;
            } else {
                System.out.println("");
            }
            NotificationSeverityType nst = n.getSeverity();

            System.out.println("    Severity: " + (nst == null ? "null" : nst.getValue()));
            System.out.println("    Code: " + n.getCode());
            System.out.println("    Message: " + n.getMessage());
            System.out.println("    Source: " + n.getSource());
        }
    }

    private static boolean isResponseOk(NotificationSeverityType notificationSeverityType) {
        if (notificationSeverityType == null) {
            return false;
        }
        if (notificationSeverityType.equals(NotificationSeverityType.WARNING) ||
                notificationSeverityType.equals(NotificationSeverityType.NOTE) ||
                notificationSeverityType.equals(NotificationSeverityType.SUCCESS)) {
            return true;
        }
        return false;
    }

    private static void print(String msg, Object obj) {
        if (msg == null || obj == null) {
            return;
        }
        System.out.println(msg + ": " + obj.toString());
    }

    private static void printMoney(String msg, Money money) {
        if (msg == null || money == null) {
            return;
        }
        System.out.println(msg + ": " + money.getAmount() + " " + money.getCurrency());
    }

    private static void printWeight(String msg, Weight weight) {
        if (msg == null || weight == null) {
            return;
        }
        System.out.println(msg + ": " + weight.getValue() + " " + weight.getUnits());
    }

    private static void updateEndPoint(RateServiceLocator serviceLocator) {
        String endPoint = System.getProperty("endPoint");
        endPoint = "https://ws.fedex.com:443/web-services/rate";

        if (endPoint != null) {
            serviceLocator.setRateServicePortEndpointAddress(endPoint);
        }
    }

}
