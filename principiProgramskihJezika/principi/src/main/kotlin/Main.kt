import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import org.example.ParkingLocation
import org.example.LocationCoordinates
import java.time.LocalDateTime
import java.util.*

@Composable
@Preview
fun App() {
    var selectedEntity by remember { mutableStateOf<String?>(null) }

    var users by remember { mutableStateOf(mutableListOf<User>()) }
    var parkingLocations by remember { mutableStateOf(mutableListOf<ParkingLocation>()) }

    if (selectedEntity == null) {
        EntitySelectionScreen(onEntitySelected = { selectedEntity = it })
    } else {
        when (selectedEntity) {
            "User" -> UserAdminUI(
                users = users,
                onAddUser = { newUser ->
                    users = users.toMutableList().apply { add(newUser) }
                },
                onUpdateUser = { updatedUser ->
                    users = users.map {
                        if (it.id == updatedUser.id) updatedUser else it
                    }.toMutableList()
                },
                onDeleteUser = { userToDelete ->
                    users = users.filter { it.id != userToDelete.id }.toMutableList()
                },
                onBack = { selectedEntity = null }
            )

            "ParkingLocation" -> ParkingLocationAdminUI(
                locations = parkingLocations,
                onAddLocation = { newLocation ->
                    parkingLocations = parkingLocations.toMutableList().apply { add(newLocation) }
                },
                onUpdateLocation = { updatedLocation ->
                    parkingLocations = parkingLocations.map {
                        if (it.id == updatedLocation.id) updatedLocation else it
                    }.toMutableList()
                },
                onDeleteLocation = { locationToDelete ->
                    parkingLocations = parkingLocations.filter { it.id != locationToDelete.id }.toMutableList()
                },
                onBack = { selectedEntity = null }
            )

            else -> PlaceholderScreen(entity = selectedEntity!!, onBack = { selectedEntity = null })
        }
    }
}



@Composable
fun EntitySelectionScreen(onEntitySelected: (String) -> Unit) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Select Entity", fontSize = 24.sp)
            Spacer(Modifier.height(20.dp))

            listOf("User", "ParkingLocation", "Vehicle", "Review").forEach { entity ->
                Button(
                    onClick = { onEntitySelected(entity) },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(entity)
                }
            }
        }
    }
}

@Composable
fun UserAdminUI(
    users: List<User>,
    onAddUser: (User) -> Unit,
    onUpdateUser: (User) -> Unit,
    onDeleteUser: (User) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Add person") }
    var selectedUserForEdit by remember { mutableStateOf<User?>(null) }

    MaterialTheme {
        Column {
            TopAppBar(
                title = { Text("User Admin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = Color(0xFFEEEEEE)
            )

            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .width(180.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFF5F5F5))
                        .padding(10.dp)
                ) {
                    NavigationButton("Add person") { selectedTab = it }
                    NavigationButton("People") { selectedTab = it }
                    NavigationButton("Scraper") { selectedTab = it }
                    NavigationButton("Generator") { selectedTab = it }
                    Spacer(Modifier.weight(1f))
                    NavigationButton("About") { selectedTab = it }
                }
                Box(Modifier.fillMaxSize().padding(16.dp)) {
                    when {
                        selectedUserForEdit != null -> EditUserScreen(
                            user = selectedUserForEdit!!,
                            onSave = {
                                onUpdateUser(it)
                                selectedUserForEdit = null
                            },
                            onDelete = {
                                onDeleteUser(it)
                                selectedUserForEdit = null
                            },
                            onBack = { selectedUserForEdit = null }
                        )
                        selectedTab == "Add person" -> AddUserScreen(onAddUser)
                        selectedTab == "People" -> PeopleScreen(users) { selectedUserForEdit = it }
                        else -> Text("Coming soon...")
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(entity: String, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("UI for '$entity' is not implemented yet", fontSize = 20.sp, color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
fun NavigationButton(label: String, onClick: (String) -> Unit) {
    Button(
        onClick = { onClick(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
    ) {
        Text(label, color = Color.Black)
    }
}

@Composable
fun AddUserScreen(onAddUser: (User) -> Unit) {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var creditCard by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        fun validateAndSubmit() {
            val user = User(
                name = name,
                surname = surname,
                username = username,
                email = email,
                password_hash = password,
                phone_number = phoneNumber,
                credit_card_number = creditCard,
                user_type = userType,
                created_at = LocalDateTime.now(),
                updated_at = LocalDateTime.now()
            )

            when {
                !user.isUsernameValid() -> errorMessage = "Invalid username (min 3 characters)"
                !user.isEmailValid() -> errorMessage = "Invalid email format"
                !user.isPhoneNumberValid() -> errorMessage = "Invalid phone number"
                !user.isCreditCardValid() -> errorMessage = "Invalid credit card number"
                !user.isUserTypeValid() -> errorMessage = "User type must be 'admin' or 'user'"
                else -> {
                    onAddUser(user)
                    name = ""
                    surname = ""
                    username = ""
                    email = ""
                    password = ""
                    phoneNumber = ""
                    creditCard = ""
                    userType = ""
                    errorMessage = null
                }
            }
        }

        TextFieldWithLabel("First Name", name) { name = it }
        TextFieldWithLabel("Last Name", surname) { surname = it }
        TextFieldWithLabel("Username", username) { username = it }
        TextFieldWithLabel("Email", email) { email = it }
        TextFieldWithLabel("Password", password, isPassword = true) { password = it }
        TextFieldWithLabel("Phone Number", phoneNumber) { phoneNumber = it }
        TextFieldWithLabel("Credit Card Number", creditCard) { creditCard = it }
        TextFieldWithLabel("User Type (admin/user)", userType) { userType = it }

        Spacer(Modifier.height(16.dp))

        errorMessage?.let {
            Text(it, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
        }

        Button(
            onClick = { validateAndSubmit() },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00796B)),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Save", color = Color.White)
        }

        Spacer(Modifier.height(50.dp))
    }
}

@Composable
fun TextFieldWithLabel(
    label: String,
    value: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
fun PeopleScreen(users: List<User>, onUserClick: (User) -> Unit) {
    var filterText by remember { mutableStateOf("") }

    val filteredUsers = users.filter {
        it.name?.contains(filterText, ignoreCase = true) == true ||
                it.surname?.contains(filterText, ignoreCase = true) == true ||
                it.username?.contains(filterText, ignoreCase = true) == true
    }

    Column {
        TextField(
            value = filterText,
            onValueChange = { filterText = it },
            label = { Text("Search by name, surname, or username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredUsers) { user ->
                UserCard(user, onClick = { onUserClick(user) })
            }
        }
    }
}

@Composable
fun UserCard(user: User, onClick: () -> Unit) {
    Card(
        elevation = 8.dp,
        modifier = Modifier
            .size(180.dp)
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF00796B))
            Spacer(Modifier.height(10.dp))
            Text(user.name ?: "", fontSize = 16.sp)
            Text(user.surname ?: "", fontSize = 20.sp, color = Color.Black)
        }
    }
}

@Composable
fun EditUserScreen(
    user: User,
    onSave: (User) -> Unit,
    onDelete: (User) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(user.name.orEmpty()) }
    var surname by remember { mutableStateOf(user.surname.orEmpty()) }
    var username by remember { mutableStateOf(user.username.orEmpty()) }
    var email by remember { mutableStateOf(user.email.orEmpty()) }
    var password by remember { mutableStateOf(user.password_hash.orEmpty()) }
    var phone by remember { mutableStateOf(user.phone_number.orEmpty()) }
    var creditCard by remember { mutableStateOf(user.credit_card_number.orEmpty()) }
    var userType by remember { mutableStateOf(user.user_type.orEmpty()) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Edit User", fontSize = 20.sp)

        Spacer(Modifier.height(8.dp))
        TextField(value = name, onValueChange = { name = it }, label = { Text("First Name") })

        Spacer(Modifier.height(8.dp))
        TextField(value = surname, onValueChange = { surname = it }, label = { Text("Last Name") })

        Spacer(Modifier.height(8.dp))
        TextField(value = username, onValueChange = { username = it }, label = { Text("Username") })

        Spacer(Modifier.height(8.dp))
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })

        Spacer(Modifier.height(8.dp))
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") })

        Spacer(Modifier.height(8.dp))
        TextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })

        Spacer(Modifier.height(8.dp))
        TextField(value = creditCard, onValueChange = { creditCard = it }, label = { Text("Credit Card") })

        Spacer(Modifier.height(8.dp))
        TextField(value = userType, onValueChange = { userType = it }, label = { Text("User Type") })

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = {
                onSave(
                    user.copy(
                        name = name,
                        surname = surname,
                        username = username,
                        email = email,
                        password_hash = password,
                        phone_number = phone,
                        credit_card_number = creditCard,
                        user_type = userType
                    )
                )
            }) {
                Text("Save")
            }

            Spacer(Modifier.width(16.dp))

            Button(onClick = { onDelete(user) }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)) {
                Text("Delete", color = Color.White)
            }

            Spacer(Modifier.width(16.dp))

            Button(onClick = onBack) {
                Text("Cancel")
            }
        }
    }
}



@Composable
fun ParkingLocationAdminUI(
    locations: List<ParkingLocation>,
    onAddLocation: (ParkingLocation) -> Unit,
    onUpdateLocation: (ParkingLocation) -> Unit,
    onDeleteLocation: (ParkingLocation) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Add location") }
    var selectedLocationForEdit by remember { mutableStateOf<ParkingLocation?>(null) }

    MaterialTheme {
        Column {
            TopAppBar(
                title = { Text("Parking Location Admin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = Color(0xFFEEEEEE)
            )
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .width(180.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFF5F5F5))
                        .padding(10.dp)
                ) {
                    NavigationButton("Add location") { selectedTab = it }
                    NavigationButton("Locations") { selectedTab = it }
                    Spacer(Modifier.weight(1f))
                    NavigationButton("About") { selectedTab = it }
                }

                Box(Modifier.fillMaxSize().padding(16.dp)) {
                    when {
                        selectedLocationForEdit != null -> EditParkingLocationScreen(
                            location = selectedLocationForEdit!!,
                            onSave = {
                                onUpdateLocation(it)
                                selectedLocationForEdit = null
                            },
                            onDelete = {
                                onDeleteLocation(it)
                                selectedLocationForEdit = null
                            },
                            onBack = { selectedLocationForEdit = null }
                        )
                        selectedTab == "Add location" -> AddParkingLocationScreen(onAddLocation)
                        selectedTab == "Locations" -> ParkingLocationListScreen(locations) { selectedLocationForEdit = it }
                    }
                }
            }
        }
    }
}
@Composable
fun AddParkingLocationScreen(onAddLocation: (ParkingLocation) -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var totalRegular by remember { mutableStateOf("0") }
    var totalInvalid by remember { mutableStateOf("0") }
    var totalBus by remember { mutableStateOf("0") }
    var availableRegular by remember { mutableStateOf("0") }
    var availableInvalid by remember { mutableStateOf("0") }
    var availableBus by remember { mutableStateOf("0") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    fun parseInt(value: String): Int = value.toIntOrNull() ?: -1

    fun validateAndSubmit() {
        val location = ParkingLocation(
            name = name,
            address = address,
            location = LocationCoordinates(
                coordinates = listOf(0.0, 0.0)
            ),
            total_regular_spots = parseInt(totalRegular),
            total_invalid_spots = parseInt(totalInvalid),
            total_bus_spots = parseInt(totalBus),
            available_regular_spots = parseInt(availableRegular),
            available_invalid_spots = parseInt(availableInvalid),
            available_bus_spots = parseInt(availableBus),
            created = LocalDateTime.now()
        )

        when {
            name.length !in 2..100 -> errorMessage = "Name must be between 2 and 100 characters."
            address.length !in 2..100 -> errorMessage = "Address must be between 2 and 100 characters."
            listOf(totalRegular, totalInvalid, totalBus, availableRegular, availableInvalid, availableBus).any { parseInt(it) < 0 } -> {
                errorMessage = "All spot values must be 0 or greater."
            }
            parseInt(availableRegular) > parseInt(totalRegular) ||
                    parseInt(availableInvalid) > parseInt(totalInvalid) ||
                    parseInt(availableBus) > parseInt(totalBus) -> {
                errorMessage = "Available spots cannot exceed total spots."
            }
            else -> {
                onAddLocation(location)
                // Clear all fields
                name = ""
                address = ""
                totalRegular = "0"
                totalInvalid = "0"
                totalBus = "0"
                availableRegular = "0"
                availableInvalid = "0"
                availableBus = "0"
                errorMessage = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        TextFieldWithLabel("Name", name) { name = it }
        TextFieldWithLabel("Address", address) { address = it }
        Spacer(Modifier.height(8.dp))

        Text("Total Spots", style = MaterialTheme.typography.subtitle1)
        RowInputs(totalRegular, { totalRegular = it }, totalInvalid, { totalInvalid = it }, totalBus, { totalBus = it })

        Spacer(Modifier.height(8.dp))

        Text("Available Spots", style = MaterialTheme.typography.subtitle1)
        RowInputs(availableRegular, { availableRegular = it }, availableInvalid, { availableInvalid = it }, availableBus, { availableBus = it })

        Spacer(Modifier.height(16.dp))

        errorMessage?.let {
            Text(it, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
        }

        Button(
            onClick = { validateAndSubmit() },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00796B)),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Save", color = Color.White)
        }
    }
}

@Composable
fun RowInputs(
    regular: String, onRegularChange: (String) -> Unit,
    invalid: String, onInvalidChange: (String) -> Unit,
    bus: String, onBusChange: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = regular,
            onValueChange = onRegularChange,
            label = { Text("Regular") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = invalid,
            onValueChange = onInvalidChange,
            label = { Text("Invalid") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = bus,
            onValueChange = onBusChange,
            label = { Text("Bus") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}


@Composable
fun ParkingLocationListScreen(locations: List<ParkingLocation>, onLocationClick: (ParkingLocation) -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        locations.forEach { location ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onLocationClick(location) },
                elevation = 4.dp
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF00796B))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(location.name, fontSize = 18.sp)
                        Text(location.address, color = Color.Gray)
                    }
                }
            }
        }
    }
}
@Composable
fun EditParkingLocationScreen(
    location: ParkingLocation,
    onSave: (ParkingLocation) -> Unit,
    onDelete: (ParkingLocation) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(location.name) }
    var address by remember { mutableStateOf(location.address) }
    var totalRegular by remember { mutableStateOf(location.total_regular_spots.toString()) }
    var totalInvalid by remember { mutableStateOf(location.total_invalid_spots.toString()) }
    var totalBus by remember { mutableStateOf(location.total_bus_spots.toString()) }
    var availableRegular by remember { mutableStateOf(location.available_regular_spots.toString()) }
    var availableInvalid by remember { mutableStateOf(location.available_invalid_spots.toString()) }
    var availableBus by remember { mutableStateOf(location.available_bus_spots.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    fun parseIntSafe(value: String) = value.toIntOrNull() ?: -1

    fun validateAndSubmit() {
        val updated = location.copy(
            name = name,
            address = address,
            total_regular_spots = parseIntSafe(totalRegular),
            total_invalid_spots = parseIntSafe(totalInvalid),
            total_bus_spots = parseIntSafe(totalBus),
            available_regular_spots = parseIntSafe(availableRegular),
            available_invalid_spots = parseIntSafe(availableInvalid),
            available_bus_spots = parseIntSafe(availableBus),
            modified = LocalDateTime.now()
        )

        when {
            name.length !in 2..100 -> errorMessage = "Name must be between 2 and 100 characters."
            address.length !in 2..100 -> errorMessage = "Address must be between 2 and 100 characters."
            listOf(totalRegular, totalInvalid, totalBus, availableRegular, availableInvalid, availableBus).any { parseIntSafe(it) < 0 } ->
                errorMessage = "All spot values must be 0 or greater."
            parseIntSafe(availableRegular) > parseIntSafe(totalRegular) ||
                    parseIntSafe(availableInvalid) > parseIntSafe(totalInvalid) ||
                    parseIntSafe(availableBus) > parseIntSafe(totalBus) ->
                errorMessage = "Available spots cannot exceed total spots."
            else -> {
                onSave(updated)
                errorMessage = null
            }
        }
    }

    Column(modifier = Modifier.verticalScroll(scrollState).padding(16.dp)) {
        TextFieldWithLabel("Name", name) { name = it }
        TextFieldWithLabel("Address", address) { address = it }

        Spacer(Modifier.height(8.dp))
        Text("Total Spots", style = MaterialTheme.typography.subtitle1)
        RowInputs(totalRegular, { totalRegular = it }, totalInvalid, { totalInvalid = it }, totalBus, { totalBus = it })

        Spacer(Modifier.height(8.dp))
        Text("Available Spots", style = MaterialTheme.typography.subtitle1)
        RowInputs(availableRegular, { availableRegular = it }, availableInvalid, { availableInvalid = it }, availableBus, { availableBus = it })

        Spacer(Modifier.height(16.dp))

        errorMessage?.let {
            Text(it, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { validateAndSubmit() }) {
                Text("Save")
            }
            Button(onClick = { onDelete(location) }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)) {
                Text("Delete", color = Color.White)
            }
            Button(onClick = onBack) {
                Text("Cancel")
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Compose Database Admin") {
        App()
    }
}
