package com.noev.ethereumhelloworld

import android.databinding.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import java.util.concurrent.TimeUnit

const val MINIMUM_GAS_LIMIT = 21000
const val PRIVATE_KEY_ROPSTEN = "YOUR_ROPSTEN_PRIVATE_KEY" //todo: You have to create an ethereum account on the Ropsten network and put your private key here
const val ROPSTEN_INFURA_URL = "https://ropsten.infura.io/YOUR_API_KEY" //todo: You have to register on the Infura website and put your api key here
const val CONTRACT_ADDRESS = "0x024b64940518779068e57352F3bDDdE08E4D9c40"

class MainActivity : AppCompatActivity() {

    val isLoading = ObservableBoolean()
    val textReadFromContract = ObservableField<String>()
    val gasPrice = ObservableInt(10)
    val userText = ObservableField<String>()
    val gasLimit = ObservableInt(MINIMUM_GAS_LIMIT)

    private var web3j: Web3j? = null

    private val credentials = Credentials.create(PRIVATE_KEY_ROPSTEN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil.setContentView<ViewDataBinding>(this, R.layout.activity_main)
        binding.setVariable(BR.viewModel, this)

        CoroutineScope(Dispatchers.Main).launch(Dispatchers.IO) {
            isLoading.set(true)
            val result = initializeWeb3J()
            isLoading.set(false)
            toast(result)
        }
    }

    fun writeButtonClicked() {
        CoroutineScope(Dispatchers.Main).launch(Dispatchers.IO) {
            isLoading.set(true)
            val result = writeToContract()
            isLoading.set(false)
            toast(result)
        }
    }

    fun readButtonClicked() {
        CoroutineScope(Dispatchers.Main).launch(Dispatchers.IO) {
            isLoading.set(true)
            val result = readFromContract()
            isLoading.set(false)
            textReadFromContract.set(result)
        }
    }

    private fun writeToContract(): String {
        val greetingToWrite = userText.get()
        val result: String
        result = try {
            val greeter = Greeter.load(CONTRACT_ADDRESS, web3j, credentials, getGasPrice(), getGasLimit())
            val transactionReceipt = greeter.changeGreeting(greetingToWrite).sendAsync().get(3, TimeUnit.MINUTES)
            "Successful transaction. Gas used: " + transactionReceipt.gasUsed
        } catch (e: Exception) {
            "Error during transaction. Error: " + e.message
        }
        return result
    }

    private fun readFromContract(): String {
        val result: String
        result = try {
            val greeter = Greeter.load(CONTRACT_ADDRESS, web3j, credentials, getGasPrice(), getGasLimit())
            val greeting = greeter.greet().sendAsync()
            greeting.get()
        } catch (e: Exception) {
            "Error reading the smart contract. Error: " + e.message
        }
        return result
    }

    private fun initializeWeb3J(): String {
        val infuraHttpService: HttpService
        val result: String
        result = try {
            infuraHttpService = HttpService(ROPSTEN_INFURA_URL)
            web3j = Web3j.build(infuraHttpService)
            "Success initializing web3j/infura"
        } catch (e: Exception) {
            val exception = e.toString()
            "Error initializing web3j/infura. Error: $exception"
        }

        return result
    }

    private fun getGasPrice(): BigInteger {
        val gasPriceGwei = gasPrice.get()
        val gasPriceWei = BigInteger.valueOf(gasPriceGwei + 1000000000L)
        Log.d("wat", "getGasPrice: $gasPriceGwei")
        return gasPriceWei
    }

    private fun getGasLimit(): BigInteger {
        return gasLimit.get().bigInteger()
    }

    private fun Int.bigInteger(): BigInteger {
        return BigInteger(toString())
    }

    private fun toast(text: String) {
        runOnUiThread {
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        }
    }
}
