package com.example.noev.greeter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.infura.InfuraHttpService;

import java.math.BigInteger;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Main Ethereum Network
 * https://mainnet.infura.io/[your-token]
 * <p>
 * Test Ethereum Network (Ropsten)
 * https://ropsten.infura.io/[your-token]
 * <p>
 * Test Rinkeby Network
 * https://rinkeby.infura.io/[your-token]
 * <p>
 * IPFS Gateway
 * https://ipfs.infura.io
 * <p>
 * IPFS RPC
 * https://ipfs.infura.io:5001
 */
public class MainActivity extends AppCompatActivity {

    private final static String privateKeyRopsten = "YOUR_PRIVATE_KEY";
    private final static String greeterContractAddressRopsten = "0x024b64940518779068e57352F3bDDdE08E4D9c40";
    private final static String ropstenUrl = "https://ropsten.infura.io/YOUR_API_KEY";

    private ProgressBar progressBar;
    private EditText editText;
    private TextView greetingTextView;
    private TextView gasPriceTextView;
    private TextView gasLimitTextView;
    private SeekBar gasPriceSeekBar;

    private Web3j web3j;

    private Credentials credentials = Credentials.create(privateKeyRopsten);
    private int minimumGasLimit = 21000;
    private BigInteger gasLimit = new BigInteger(String.valueOf(minimumGasLimit));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUi();
        setGasPriceText(10);
        setGasLimit(minimumGasLimit);
        initWeb3j();
    }

    private void initUi() {
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        editText = (EditText) findViewById(R.id.edit_text);
        greetingTextView = (TextView) findViewById(R.id.text);
        Button readButton = (Button) findViewById(R.id.button);
        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getGreeting();
            }
        });
        Button writeButton = (Button) findViewById(R.id.write_button);
        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeGreetingToContract();
            }
        });
        gasPriceSeekBar = (SeekBar) findViewById(R.id.gas_price_seek_bar);
        gasPriceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setGasPriceText(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        SeekBar gasLimitSeekBar = (SeekBar) findViewById(R.id.gas_limit_seek_bar);
        gasLimitSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setGasLimit(progress + minimumGasLimit);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        gasLimitTextView = (TextView) findViewById(R.id.gas_limit_text_view);
        gasPriceTextView = (TextView) findViewById(R.id.gas_price_text_view);
    }

    private void writeGreetingToContract() {
        progressBar.setVisibility(View.VISIBLE);
        WriteTask writeTask = new WriteTask();
        writeTask.execute(editText.getText().toString());
    }

    private void getGreeting() {
        try {
            progressBar.setVisibility(View.VISIBLE);
            ReadTask readTask = new ReadTask();
            readTask.execute();
        } catch (Exception e) {
            Log.d("wat", "getGreeting exception = " + e.getMessage());
        }
    }

    private void initWeb3j() {
        InitWeb3JTask task = new InitWeb3JTask();
        task.execute(ropstenUrl);
    }

    public void setGasPriceText(int gasPrice) {
        String formattedString = getString(R.string.gas_price, String.valueOf(gasPrice));
        gasPriceTextView.setText(formattedString);
    }

    private BigInteger getGasPrice() {
        int gasPriceGwei = gasPriceSeekBar.getProgress();
        BigInteger gasPriceWei = BigInteger.valueOf(gasPriceGwei + 1000000000L);
        Log.d("wat", "getGasPrice: " + String.valueOf(gasPriceGwei));
        return gasPriceWei;
    }

    public void setGasLimit(int gasLimit) {
        String gl = String.valueOf(gasLimit);
        this.gasLimit = new BigInteger(gl);
        gasLimitTextView.setText(getString(R.string.gas_limit, gl));
    }

    private class ReadTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String result;
            try {
                Greeter greeter = Greeter.load(greeterContractAddressRopsten, web3j, credentials, getGasPrice(), gasLimit);
                Future<Utf8String> greeting = greeter.greet();
                Utf8String greetingUtf8 = greeting.get();
                result = greetingUtf8.getValue();
            } catch (Exception e) {
                result = "Error reading the smart contract. Error: " + e.getMessage();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressBar.setVisibility(View.INVISIBLE);
            greetingTextView.setText(result);
        }
    }


    private class WriteTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String greetingToWrite = params[0];

            String result;
            try {
                Greeter greeter = Greeter.load(greeterContractAddressRopsten, web3j, credentials, getGasPrice(), gasLimit);
                TransactionReceipt transactionReceipt = greeter.changeGreeting(new Utf8String(greetingToWrite)).get(3, TimeUnit.MINUTES);
                result = "Successful transaction. Gas used: " + transactionReceipt.getGasUsed();
            } catch (Exception e) {
                result = "Error during transaction. Error: " + e.getMessage();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
        }
    }

    private class InitWeb3JTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String url = params[0];
            InfuraHttpService infuraHttpService;
            String result = "Success initializing web3j/infura";
            try {
                infuraHttpService = new InfuraHttpService(url);
                web3j = Web3jFactory.build(infuraHttpService);
            } catch (Exception wtf) {
                String exception = wtf.toString();
                Log.d("wat", "Error initializing web3j/infura. Error: " + exception);
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
        }
    }
}
