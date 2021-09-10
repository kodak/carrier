# Carrier 📭
> A Redis PubSub implementation that allows for super simple handling of messages in your java application.

## Usage
### Setting up Carrier in your java application
To be able to send and receive Carrier message in your application you first need to do some small setup.
Firstly, ensure you have setup a JedisPool and Gson instance. Then you are ready to instantiate your Carrier instance.
```Java
    Carrier carrier = new Carrier("Channel", jedisPool, gson);
```

### Sending a Carrier message
Sending a Carrier message is super simple.
All you need to do is use your Carrier instance's sendMessage method to specify a Message id and the data you wish to send, the Carrier message handler will do the rest of the work. 
```Java
    Map<String, String> data = new HashMap<>();
    myData.put("project", "Carrier");
    
    Message message = new Message("CARRIER", data);
    carrier.sendMessage(message);
```

### Receiving a Carrier message
Now that we've sent our Carrier message, all we need to do is handle it. Let's create the class that contains our handler methods.

```Java
    public class CarrierMessageListeners implements MessageListener {
        
    }
```
Ensure your class implements MessageListener!
Next, we'll create the method inside this class that handlers our "CARRIER" message that we sent earlier. 
We can do this by using the @IncomingMessageHandler annotation on our method.
```Java
    @IncomingMessageHandler("CARRIER")
    public void onCarrier(JsonObject data) {
        // Now we can do whatever we like with that data we sent earlier!
        System.out.println("Project: " + data.get("project").getAsString());
    }
```
Then, finally, we just need to register the listener class with our Carrier instance and that's as simple as...
```Java
    carrier.registerListener(new CarrierMessageListeners());
```

And that's it! Carrier will do the rest. You now know how to leverage the full power of the Carrier Redis PubSub Message handler.
Also Make sure to use Carrier#close when your application is stopping, to ensure the pubsub and jedis pool are closed correctly.

## Get the Source
 1. Install maven `sudo apt-get install maven`
 2. Verify installation `mvn -v`
 3. Clone the repository `git clone git@github.com:kodak/carrier.git`
 4. Navigate to the new folder `cd carrier`
 5. Import `pom.xml` into your IDE

## Compile a Build
 1. Navigate to the repository home directory
 2. Run `mvn clean install`
 3. Find the compiled jars inside the target directory

## Report an Issue
If you find an issue you can submit it [here](https://github.com/kodak/carrier/issues).

## Contributing
You can submit a [pull request](https://github.com/kodak/carrier/pulls) with your changes.
