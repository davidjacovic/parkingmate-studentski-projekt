import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import org.example.*
import java.time.LocalDateTime
import androidx.compose.foundation.lazy.items
import java.math.BigDecimal
import java.util.*
import skraper.main
import org.bson.types.ObjectId
import org.example.db.*


@Composable
@Preview
fun App() {
    var selectedEntity by remember { mutableStateOf<String?>(null) }

    var users by remember { mutableStateOf(mutableListOf<User>()) }
    var parkingLocations by remember { mutableStateOf(mutableListOf<ParkingLocation>()) }
    var subscribers by remember { mutableStateOf(mutableListOf<Subscriber>()) }
    var tariffs by remember { mutableStateOf(mutableListOf<Tariff>()) }
    var reviews by remember { mutableStateOf(mutableListOf<Review>()) }
    var payments by remember { mutableStateOf(mutableListOf<Payment>()) }

    val userRepo = UserRepository()



    if (selectedEntity == null) {
        EntitySelectionScreen(onEntitySelected = { selectedEntity = it })
    } else {
        when (selectedEntity) {
            "User" -> UserAdminUI(
                users = users,
                onAddUser = { newUser ->
                    userRepo.insert(newUser)
                    users = userRepo.findAll().toMutableList() // Osveži prikaz liste iz baze
                },
                onUpdateUser = { updatedUser ->
                    userRepo.update(updatedUser)
                    users = userRepo.findAll().toMutableList()
                },
                onDeleteUser = { userToDelete ->
                    userRepo.deleteById(userToDelete.id.toHexString())
                    users = userRepo.findAll().toMutableList()
                },
                onBack = { selectedEntity = null }
            )

            "ParkingLocation" -> ParkingLocationAdminUI(
                locations = parkingLocations,
                subscribers = subscribers,
                tariffs = tariffs,
                onAddLocation = { newLocation, newSubscriber, newTariffs ->
                    subscribers = subscribers.toMutableList().apply { add(newSubscriber) }
                    parkingLocations = parkingLocations.toMutableList().apply {
                        add(newLocation.copy(subscriber = newSubscriber.id))
                    }
                    tariffs = tariffs.toMutableList().apply { addAll(newTariffs) }
                },
                onUpdateLocation = { updatedLocation, updatedSubscriber ->
                    parkingLocations = parkingLocations.map {
                        if (it.id == updatedLocation.id) updatedLocation else it
                    }.toMutableList()
                    subscribers = subscribers.map {
                        if (it.id == updatedSubscriber.id) updatedSubscriber else it
                    }.toMutableList()
                },
                onDeleteLocation = { locationToDelete ->
                    parkingLocations = parkingLocations.filter { it.id != locationToDelete.id }.toMutableList()
                },
                onAddTariff = { newTariff ->
                    tariffs = tariffs.toMutableList().apply { add(newTariff) }
                },
                onDeleteTariff = { tariffToDelete ->
                    tariffs = tariffs.filter { it.id != tariffToDelete.id }.toMutableList()
                },
                onBack = { selectedEntity = null }
            )
            "Review" -> ReviewAdminUI(
                users = users,
                locations = parkingLocations,
                reviews = reviews,
                onAddReview = { newReview ->
                    reviews = reviews.toMutableList().apply { add(newReview) }
                },
                onBack = { selectedEntity = null }
            )
            "Payment" -> PaymentAdminUI(
                users = users,
                parkingLocations = parkingLocations,
                tariffs = tariffs,
                payments = payments,
                onAddPayment = { newPayment ->
                    payments = payments.toMutableList().apply { add(newPayment) }
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

            listOf("User", "ParkingLocation", "Payment", "Review").forEach { entity ->
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
fun <T> DropdownSelector(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    displayText: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text(selected?.let(displayText) ?: "Select")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(onClick = {
                    onSelect(option)
                    expanded = false
                }) {
                    Text(displayText(option))
                }
            }
        }
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
                        selectedTab == "Add person" -> AddUserScreen(
                            onAddUser = onAddUser,
                            onBack = { selectedTab = "People" }
                        )
                        selectedTab == "People" -> PeopleScreen(users) { selectedUserForEdit = it }
                        else -> Text("Coming soon...")
                    }
                }
            }
        }
    }
}

@Composable
fun AddUserScreen(
    onAddUser: (User) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var creditCard by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf("") }

    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var editingVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var isAddingVehicle by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    if (editingVehicle != null || isAddingVehicle) {
        AddOrEditVehicleScreen(
            vehicle = editingVehicle,
            onSave = { vehicle ->
                vehicles = if (editingVehicle != null) {
                    vehicles.map { if (it.id == vehicle.id) vehicle else it }
                } else {
                    vehicles + vehicle
                }
                editingVehicle = null
                isAddingVehicle = false
            },
            onDelete = {
                vehicles = vehicles.filter { it.id != editingVehicle?.id }
                editingVehicle = null
                isAddingVehicle = false
            },
            onBack = {
                editingVehicle = null
                isAddingVehicle = false
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text("Add User", fontSize = 20.sp)

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
                    updated_at = LocalDateTime.now(),
                    vehicles = vehicles
                )

                when {
                    !user.isUsernameValid() -> errorMessage = "Invalid username (min 8 characters)"
                    !user.isEmailValid() -> errorMessage = "Invalid email format"
                    !user.isPasswordValid()-> errorMessage = "You need stronger password"
                    !user.isPhoneNumberValid() -> errorMessage = "Invalid phone number"
                    !user.isCreditCardValid() -> errorMessage = "Invalid credit card number"
                    !user.isUserTypeValid() -> errorMessage = "User type must be 'admin' or 'user'"
                    vehicles.isEmpty() -> errorMessage = "You must add at least one vehicle"
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
                        vehicles = emptyList()
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

            Text("Vehicles", fontSize = 18.sp)

            vehicles.forEach { vehicle ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { editingVehicle = vehicle },
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = vehicle.registration_number ?: "No registration",
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = vehicle.vehicle_type ?: "Unknown type",
                                style = MaterialTheme.typography.body2,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { editingVehicle = vehicle }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Vehicle")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(onClick = { isAddingVehicle = true }) {
                Text("Add Vehicle")
            }

            errorMessage?.let {
                Text(it, color = Color.Red, modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { validateAndSubmit() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00796B))
                ) {
                    Text("Save", color = Color.White)
                }

                Button(onClick = onBack) {
                    Text("Cancel")
                }
            }

            Spacer(Modifier.height(50.dp))
        }
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
    var vehicles by remember { mutableStateOf(user.vehicles.toMutableList()) }
    var editingVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var isAddingVehicle by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    fun validateAndSave() {
        val updatedUser = user.copy(
            name = name,
            surname = surname,
            username = username,
            email = email,
            password_hash = password,
            phone_number = phone,
            credit_card_number = creditCard,
            user_type = userType,
            updated_at = LocalDateTime.now(),
            vehicles = vehicles
        )

        when {
            !updatedUser.isUsernameValid() -> errorMessage = "Invalid username (min 8 characters)"
            !updatedUser.isEmailValid() -> errorMessage = "Invalid email format"
            !updatedUser.isPasswordValid() -> errorMessage = "You need stronger password"
            !updatedUser.isPhoneNumberValid() -> errorMessage = "Invalid phone number"
            !updatedUser.isCreditCardValid() -> errorMessage = "Invalid credit card number"
            !updatedUser.isUserTypeValid() -> errorMessage = "User type must be 'admin' or 'user'"
            vehicles.isEmpty() -> errorMessage = "You must add at least one vehicle"
            else -> {
                errorMessage = null
                onSave(updatedUser)
            }
        }
    }

    if (editingVehicle != null || isAddingVehicle) {
        AddOrEditVehicleScreen(
            vehicle = editingVehicle,
            onSave = { vehicle ->
                vehicles = if (editingVehicle != null) {
                    vehicles.map { if (it.id == vehicle.id) vehicle else it }.toMutableList()
                } else {
                    vehicles.toMutableList().apply { add(vehicle) }
                }
                editingVehicle = null
                isAddingVehicle = false
            },
            onDelete = {
                vehicles = vehicles.filter { it.id != editingVehicle?.id }.toMutableList()
                editingVehicle = null
                isAddingVehicle = false
            },
            onBack = {
                editingVehicle = null
                isAddingVehicle = false
            }
        )
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text("Edit User", fontSize = 20.sp)

            TextFieldWithLabel("First Name", name) { name = it }
            TextFieldWithLabel("Last Name", surname) { surname = it }
            TextFieldWithLabel("Username", username) { username = it }
            TextFieldWithLabel("Email", email) { email = it }
            TextFieldWithLabel("Password", password, isPassword = true) { password = it }
            TextFieldWithLabel("Phone Number", phone) { phone = it }
            TextFieldWithLabel("Credit Card Number", creditCard) { creditCard = it }
            TextFieldWithLabel("User Type (admin/user)", userType) { userType = it }

            Spacer(Modifier.height(16.dp))

            Text("Vehicles", fontSize = 18.sp)

            vehicles.forEach { vehicle ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { editingVehicle = vehicle },
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = vehicle.registration_number ?: "No registration",
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = vehicle.vehicle_type ?: "Unknown type",
                                style = MaterialTheme.typography.body2,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { editingVehicle = vehicle }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Vehicle")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(onClick = { isAddingVehicle = true }) {
                Text("Add Vehicle")
            }

            errorMessage?.let {
                Text(it, color = Color.Red, modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { validateAndSave() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00796B))
                ) {
                    Text("Save", color = Color.White)
                }

                Button(
                    onClick = { onDelete(user) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }

                Button(onClick = onBack) {
                    Text("Cancel")
                }
            }

            Spacer(Modifier.height(50.dp))
        }
    }
}


@Composable
fun AddOrEditVehicleScreen(
    vehicle: Vehicle?,
    onSave: (Vehicle) -> Unit,
    onDelete: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    var registrationNumber by remember { mutableStateOf(vehicle?.registration_number ?: "") }
    var vehicleType by remember { mutableStateOf(vehicle?.vehicle_type ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(if (vehicle == null) "Add Vehicle" else "Edit Vehicle", fontSize = 20.sp)

        Spacer(Modifier.height(8.dp))
        TextField(
            value = registrationNumber,
            onValueChange = { registrationNumber = it },
            label = { Text("Registration Number") }
        )

        Spacer(Modifier.height(8.dp))
        TextField(
            value = vehicleType,
            onValueChange = { vehicleType = it },
            label = { Text("Vehicle Type (car, truck, motorcycle, bus)") }
        )

        Spacer(Modifier.height(16.dp))
        errorMessage?.let {
            Text(it, color = Color.Red)
            Spacer(Modifier.height(8.dp))
        }

        Row {
            Button(onClick = {
                val newVehicle = Vehicle(
                    registration_number = registrationNumber,
                    vehicle_type = vehicleType,
                    created = vehicle?.created ?: LocalDateTime.now(),
                    modified = LocalDateTime.now(),
                    user = vehicle?.user ?: null
                )
                if (!newVehicle.isValid()) {
                    errorMessage = "Invalid data. Check registration number and vehicle type."
                    return@Button
                }
                onSave(newVehicle)
            }) {
                Text("Save")
            }

            Spacer(Modifier.width(8.dp))

            if (vehicle != null && onDelete != null) {
                Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)) {
                    Text("Delete", color = Color.White)
                }

                Spacer(Modifier.width(8.dp))
            }

            Button(onClick = onBack) {
                Text("Cancel")
            }
        }
    }
}



@Composable
fun ParkingLocationAdminUI(
    locations: List<ParkingLocation>,
    subscribers: List<Subscriber>,
    tariffs: List<Tariff>,
    onAddLocation: (ParkingLocation, Subscriber, List<Tariff>) -> Unit,
    onUpdateLocation: (ParkingLocation, Subscriber) -> Unit,
    onDeleteLocation: (ParkingLocation) -> Unit,
    onAddTariff: (Tariff) -> Unit,
    onDeleteTariff: (Tariff) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Add location") }
    var selectedLocationForEdit by remember { mutableStateOf<ParkingLocation?>(null) }
    var selectedLocationForTariff by remember { mutableStateOf<ParkingLocation?>(null) }
    var selectedLocationForDetail by remember { mutableStateOf<ParkingLocation?>(null) }

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
                        selectedLocationForEdit != null -> {
                            val subscriber = subscribers.find { it.id == selectedLocationForEdit!!.subscriber }
                            EditParkingLocationScreen(
                                location = selectedLocationForEdit!!,
                                subscriber = subscriber,
                                tariffs = tariffs,
                                onSave = { updatedLocation, updatedSubscriber ->
                                    onUpdateLocation(updatedLocation, updatedSubscriber)
                                    selectedLocationForEdit = null
                                },
                                onDelete = {
                                    onDeleteLocation(it)
                                    selectedLocationForEdit = null
                                },
                                onBack = { selectedLocationForEdit = null },
                                onAddTariff = { _, tariff -> onAddTariff(tariff) },
                                onDeleteTariff = { onDeleteTariff(it) }
                            )
                        }
                        selectedLocationForTariff != null -> {
                            AddTariffScreen(
                                location = selectedLocationForTariff!!,
                                onSave = { tariff ->
                                    onAddTariff(tariff)
                                    selectedLocationForTariff = null
                                },
                                onCancel = { selectedLocationForTariff = null }
                            )
                        }
                        selectedTab == "Add location" -> AddParkingLocationScreen(onAddLocation)
                        selectedTab == "Locations" -> ParkingLocationListScreen(
                            locations,
                            onLocationClick = { selectedLocationForEdit = it },
                            onAddTariffClick = { selectedLocationForTariff = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddParkingLocationScreen(
    onAddLocation: (ParkingLocation, Subscriber, List<Tariff>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("0.0") }
    var latitude by remember { mutableStateOf("0.0") }

    var totalRegular by remember { mutableStateOf("0") }
    var totalInvalid by remember { mutableStateOf("0") }
    var totalBus by remember { mutableStateOf("0") }

    var availableRegular by remember { mutableStateOf("0") }
    var availableInvalid by remember { mutableStateOf("0") }
    var availableBus by remember { mutableStateOf("0") }

    var subscriberTotal by remember { mutableStateOf("0") }
    var subscriberAvailable by remember { mutableStateOf("0") }
    var subscriberReserved by remember { mutableStateOf("0") }
    var subscriberWaiting by remember { mutableStateOf("0") }

    var description by remember { mutableStateOf("") }

    var tariffs by remember { mutableStateOf(listOf<Tariff>()) }
    var showTariffForm by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    fun parseInt(value: String): Int = value.toIntOrNull() ?: -1

    fun validateAndSubmit() {
        val lat = latitude.toDoubleOrNull()
        val lon = longitude.toDoubleOrNull()

        if (lat == null || lon == null) {
            errorMessage = "Invalid coordinates."
            return
        }

        val subscriber = Subscriber(
            available_spots = parseInt(subscriberAvailable),
            total_spots = parseInt(subscriberTotal),
            reserved_spots = parseInt(subscriberReserved),
            waiting_line = parseInt(subscriberWaiting),
            created = LocalDateTime.now()
        )

        if (!subscriber.isValid()) {
            errorMessage = "Subscriber data is invalid."
            return
        }

        val location = ParkingLocation(
            name = name,
            address = address,
            location = LocationCoordinates(coordinates = listOf(lon, lat)),
            total_regular_spots = parseInt(totalRegular),
            total_invalid_spots = parseInt(totalInvalid),
            total_bus_spots = parseInt(totalBus),
            available_regular_spots = parseInt(availableRegular),
            available_invalid_spots = parseInt(availableInvalid),
            available_bus_spots = parseInt(availableBus),
            created = LocalDateTime.now(),
            subscriber = subscriber.id,
            description = if (description.isBlank()) null else description
        )

        if (!location.isValid()) {
            errorMessage = "Parking location data is invalid."
            return
        }

        val updatedTariffs = tariffs.map { it.copy(parking_location = location.id) }

        onAddLocation(location, subscriber, updatedTariffs)

        name = ""
        address = ""
        longitude = "0.0"
        latitude = "0.0"
        totalRegular = "0"
        totalInvalid = "0"
        totalBus = "0"
        availableRegular = "0"
        availableInvalid = "0"
        availableBus = "0"
        subscriberTotal = "0"
        subscriberAvailable = "0"
        subscriberReserved = "0"
        subscriberWaiting = "0"
        description = ""
        tariffs = emptyList()
        errorMessage = null
    }

    if (showTariffForm) {
        AddTariffScreen(
            location = null,
            onSave = { tariff ->
                tariffs = tariffs + tariff
                showTariffForm = false
            },
            onCancel = { showTariffForm = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        TextFieldWithLabel("Name", name) { name = it }
        TextFieldWithLabel("Address", address) { address = it }
        TextFieldWithLabel("Description", description) { description = it }

        Spacer(Modifier.height(8.dp))
        Text("Coordinates", style = MaterialTheme.typography.subtitle1)
        RowInputs2Fields(longitude, { longitude = it }, latitude, { latitude = it }, "Longitude", "Latitude")

        Spacer(Modifier.height(8.dp))
        Text("Total Spots", style = MaterialTheme.typography.subtitle1)
        RowInputs(totalRegular, { totalRegular = it }, totalInvalid, { totalInvalid = it }, totalBus, { totalBus = it })

        Spacer(Modifier.height(8.dp))
        Text("Available Spots", style = MaterialTheme.typography.subtitle1)
        RowInputs(availableRegular, { availableRegular = it }, availableInvalid, { availableInvalid = it }, availableBus, { availableBus = it })

        Spacer(Modifier.height(8.dp))
        Text("Subscriber Info", style = MaterialTheme.typography.subtitle1)
        TextFieldWithLabel("Total Spots", subscriberTotal) { subscriberTotal = it }
        TextFieldWithLabel("Available Spots", subscriberAvailable) { subscriberAvailable = it }
        TextFieldWithLabel("Reserved Spots", subscriberReserved) { subscriberReserved = it }
        TextFieldWithLabel("Waiting Line", subscriberWaiting) { subscriberWaiting = it }

        Spacer(Modifier.height(16.dp))
        Text("Tariffs", style = MaterialTheme.typography.subtitle1)
        tariffs.forEachIndexed { index, tariff ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                elevation = 4.dp
            ) {
                Row(
                    Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Type: ${tariff.tariff_type}")
                        Text("Duration: ${tariff.duration}")
                        Text("Vehicle: ${tariff.vehicle_type}")
                        Text("Price: ${tariff.price} ${tariff.price_unit}")
                    }
                    IconButton(onClick = {
                        tariffs = tariffs.toMutableList().also { it.removeAt(index) }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove tariff")
                    }
                }
            }
        }

        Button(
            onClick = { showTariffForm = true },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Add Tariff")
        }

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
fun ParkingLocationListScreen(
    locations: List<ParkingLocation>,
    onLocationClick: (ParkingLocation) -> Unit,
    onAddTariffClick: (ParkingLocation) -> Unit
) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        locations.forEach { location ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onLocationClick(location) },
                elevation = 4.dp
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
}
@Composable
fun EditParkingLocationScreen(
    location: ParkingLocation,
    subscriber: Subscriber?,
    tariffs: List<Tariff>,
    onSave: (ParkingLocation, Subscriber) -> Unit,
    onDelete: (ParkingLocation) -> Unit,
    onBack: () -> Unit,
    onAddTariff: (ParkingLocation, Tariff) -> Unit,
    onDeleteTariff: (Tariff) -> Unit
) {
    var name by remember { mutableStateOf(location.name) }
    var address by remember { mutableStateOf(location.address) }
    var description by remember { mutableStateOf(location.description ?: "") }

    var longitude by remember { mutableStateOf(location.location.coordinates.getOrNull(0)?.toString() ?: "0.0") }
    var latitude by remember { mutableStateOf(location.location.coordinates.getOrNull(1)?.toString() ?: "0.0") }

    var totalRegular by remember { mutableStateOf(location.total_regular_spots.toString()) }
    var totalInvalid by remember { mutableStateOf(location.total_invalid_spots.toString()) }
    var totalBus by remember { mutableStateOf(location.total_bus_spots.toString()) }

    var availableRegular by remember { mutableStateOf(location.available_regular_spots.toString()) }
    var availableInvalid by remember { mutableStateOf(location.available_invalid_spots.toString()) }
    var availableBus by remember { mutableStateOf(location.available_bus_spots.toString()) }

    var subscriberTotal by remember { mutableStateOf(subscriber?.total_spots?.toString() ?: "0") }
    var subscriberAvailable by remember { mutableStateOf(subscriber?.available_spots?.toString() ?: "0") }
    var subscriberReserved by remember { mutableStateOf(subscriber?.reserved_spots?.toString() ?: "0") }
    var subscriberWaiting by remember { mutableStateOf(subscriber?.waiting_line?.toString() ?: "0") }

    var showTariffForm by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    fun parseIntSafe(value: String) = value.toIntOrNull() ?: -1

    fun validateAndSubmit() {
        val lat = latitude.toDoubleOrNull()
        val lon = longitude.toDoubleOrNull()

        if (lat == null || lon == null) {
            errorMessage = "Invalid coordinates."
            return
        }

        val updatedSubscriber = Subscriber(
            available_spots = parseIntSafe(subscriberAvailable),
            total_spots = parseIntSafe(subscriberTotal),
            reserved_spots = parseIntSafe(subscriberReserved),
            waiting_line = parseIntSafe(subscriberWaiting),
            created = subscriber?.created ?: LocalDateTime.now(),
            modified = LocalDateTime.now()
        )

        if (!updatedSubscriber.isValid()) {
            errorMessage = "Subscriber data is invalid."
            return
        }

        val updatedLocation = location.copy(
            name = name,
            address = address,
            location = LocationCoordinates(coordinates = listOf(lon, lat)),
            total_regular_spots = parseIntSafe(totalRegular),
            total_invalid_spots = parseIntSafe(totalInvalid),
            total_bus_spots = parseIntSafe(totalBus),
            available_regular_spots = parseIntSafe(availableRegular),
            available_invalid_spots = parseIntSafe(availableInvalid),
            available_bus_spots = parseIntSafe(availableBus),
            modified = LocalDateTime.now(),
            subscriber = updatedSubscriber.id,
            description = if (description.isBlank()) null else description
        )

        if (!updatedLocation.isValid()) {
            errorMessage = "Parking location data is invalid."
            return
        }

        onSave(updatedLocation, updatedSubscriber)
    }

    if (showTariffForm) {
        AddTariffScreen(
            location = location,
            onSave = { tariff ->
                onAddTariff(location, tariff)
                showTariffForm = false
            },
            onCancel = { showTariffForm = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        TextFieldWithLabel("Name", name) { name = it }
        TextFieldWithLabel("Address", address) { address = it }
        TextFieldWithLabel("Description", description) { description = it }

        Spacer(Modifier.height(8.dp))
        Text("Coordinates", style = MaterialTheme.typography.subtitle1)
        RowInputs2Fields(longitude, { longitude = it }, latitude, { latitude = it }, "Longitude", "Latitude")

        Spacer(Modifier.height(8.dp))
        Text("Total Spots", style = MaterialTheme.typography.subtitle1)
        RowInputs(totalRegular, { totalRegular = it }, totalInvalid, { totalInvalid = it }, totalBus, { totalBus = it })

        Spacer(Modifier.height(8.dp))
        Text("Available Spots", style = MaterialTheme.typography.subtitle1)
        RowInputs(availableRegular, { availableRegular = it }, availableInvalid, { availableInvalid = it }, availableBus, { availableBus = it })

        Spacer(Modifier.height(8.dp))
        Text("Subscriber Info", style = MaterialTheme.typography.subtitle1)
        TextFieldWithLabel("Total Spots", subscriberTotal) { subscriberTotal = it }
        TextFieldWithLabel("Available Spots", subscriberAvailable) { subscriberAvailable = it }
        TextFieldWithLabel("Reserved Spots", subscriberReserved) { subscriberReserved = it }
        TextFieldWithLabel("Waiting Line", subscriberWaiting) { subscriberWaiting = it }

        Spacer(Modifier.height(16.dp))
        Text("Tariffs", style = MaterialTheme.typography.subtitle1)
        tariffs.forEach { tariff ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                elevation = 4.dp
            ) {
                Row(
                    Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Type: ${tariff.tariff_type}")
                        Text("Duration: ${tariff.duration}")
                        Text("Vehicle: ${tariff.vehicle_type}")
                        Text("Price: ${tariff.price} ${tariff.price_unit}")
                    }
                    IconButton(onClick = { onDeleteTariff(tariff) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove tariff")
                    }
                }
            }
        }

        Button(
            onClick = { showTariffForm = true },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Add Tariff")
        }

        Spacer(Modifier.height(16.dp))
        errorMessage?.let {
            Text(it, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)
            ) {
                Text("Cancel", color = Color.White)
            }
            Button(
                onClick = { validateAndSubmit() },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00796B))
            ) {
                Text("Save", color = Color.White)
            }
            Button(
                onClick = { onDelete(location) },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
            ) {
                Text("Delete", color = Color.White)
            }
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
fun RowInputs2Fields(
    first: String, onFirstChange: (String) -> Unit,
    second: String, onSecondChange: (String) -> Unit,
    firstLabel: String = "First",
    secondLabel: String = "Second"
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = first,
            onValueChange = onFirstChange,
            label = { Text(firstLabel) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = second,
            onValueChange = onSecondChange,
            label = { Text(secondLabel) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
fun AddTariffScreen(
    location: ParkingLocation? = null,
    onSave: (Tariff) -> Unit,
    onCancel: () -> Unit
) {
    var tariffType by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var priceUnit by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validateAndSave() {
        val priceDecimal = price.toBigDecimalOrNull()
        if (tariffType.isBlank() || duration.isBlank() || vehicleType.isBlank() || priceDecimal == null || priceUnit.isBlank()) {
            errorMessage = "Please fill in all fields with valid data"
            return
        }

        val newTariff = Tariff(
            tariff_type = tariffType,
            duration = duration,
            vehicle_type = vehicleType,
            price = priceDecimal,
            price_unit = priceUnit,
            parking_location = location?.id,
            created = LocalDateTime.now(),
            modified = LocalDateTime.now()
        )

        if (!newTariff.isValid()) {
            errorMessage = "Invalid tariff data"
            return
        }

        onSave(newTariff)
    }


    Column(modifier = Modifier.padding(16.dp)) {
        Text("Add Tariff for location: ${location?.name ?: "New Location"}", style = MaterialTheme.typography.h6)

        OutlinedTextField(
            value = tariffType,
            onValueChange = { tariffType = it },
            label = { Text("Tariff Type") }
        )

        OutlinedTextField(
            value = duration,
            onValueChange = { duration = it },
            label = { Text("Duration (HH:MM-HH:MM)") }
        )

        OutlinedTextField(
            value = vehicleType,
            onValueChange = { vehicleType = it },
            label = { Text("Vehicle Type") }
        )

        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Price") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = priceUnit,
            onValueChange = { priceUnit = it },
            label = { Text("Price Unit") }
        )

        errorMessage?.let {
            Text(it, color = Color.Red, modifier = Modifier.padding(vertical = 8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { validateAndSave() }) {
                Text("Save")
            }
            Button(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}



@Composable
fun ReviewAdminUI(
    users: List<User>,
    locations: List<ParkingLocation>,
    reviews: List<Review>,
    onAddReview: (Review) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Add review") }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var selectedLocation by remember { mutableStateOf<ParkingLocation?>(null) }
    var rating by remember { mutableStateOf(1) }
    var reviewText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    MaterialTheme {
        Column {
            TopAppBar(
                title = { Text("Review Admin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = Color(0xFFEEEEEE)
            )
            Row(Modifier.fillMaxSize()) {
                // Sidebar
                Column(
                    Modifier
                        .width(180.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFF5F5F5))
                        .padding(10.dp)
                ) {
                    NavigationButton("Add review") { selectedTab = it }
                    NavigationButton("View reviews") { selectedTab = it }
                    Spacer(Modifier.weight(1f))
                    NavigationButton("About") { selectedTab = it }
                }

                // Main content
                Box(Modifier.fillMaxSize().padding(16.dp)) {
                    when (selectedTab) {
                        "Add review" -> {
                            Column {
                                // User selector
                                Text("Select User", style = MaterialTheme.typography.subtitle1)
                                DropdownSelector(
                                    options = users,
                                    selected = selectedUser,
                                    onSelect = { selectedUser = it },
                                    displayText = { it.name ?: "Unknown" }
                                )
                                Spacer(Modifier.height(16.dp))

                                // Location selector
                                Text("Select Parking Location", style = MaterialTheme.typography.subtitle1)
                                DropdownSelector(
                                    options = locations,
                                    selected = selectedLocation,
                                    onSelect = { selectedLocation = it },
                                    displayText = { it.name }
                                )
                                Spacer(Modifier.height(16.dp))

                                // Rating selector
                                Text("Rating (1 to 5)")
                                Row {
                                    (1..5).forEach { r ->
                                        Button(
                                            onClick = { rating = r },
                                            colors = if (rating == r) ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                                            else ButtonDefaults.buttonColors()
                                        ) {
                                            Text(r.toString())
                                        }
                                        Spacer(Modifier.width(8.dp))
                                    }
                                }
                                Spacer(Modifier.height(16.dp))

                                // Review Text Input
                                OutlinedTextField(
                                    value = reviewText,
                                    onValueChange = { if (it.length <= 1000) reviewText = it },
                                    label = { Text("Review Text (max 1000 chars)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 4
                                )

                                Spacer(Modifier.height(16.dp))



                                errorMessage?.let {
                                    Text(it, color = MaterialTheme.colors.error)
                                    Spacer(Modifier.height(12.dp))
                                }

                                Button(
                                    onClick = {
                                        errorMessage = null
                                        if (selectedUser == null) {
                                            errorMessage = "Please select a User"
                                            return@Button
                                        }
                                        if (selectedLocation == null) {
                                            errorMessage = "Please select a Parking Location"
                                            return@Button
                                        }
                                        val newReview = Review(
                                            rating = rating,
                                            review_text = reviewText.takeIf { it.isNotBlank() },
                                            review_date = LocalDateTime.now(),
                                            created = LocalDateTime.now(),
                                            modified = LocalDateTime.now(),
                                            user = selectedUser!!.id,
                                            parking_location = selectedLocation!!.id
                                        )
                                        if (!newReview.isValid()) {
                                            errorMessage = "Review data is not valid."
                                            return@Button
                                        }
                                        onAddReview(newReview)

                                        // Reset form after add
                                        rating = 1
                                        reviewText = ""
                                        selectedUser = null
                                        selectedLocation = null
                                    },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00796B)),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Save Review", color = Color.White)
                                }
                            }
                        }
                        "View reviews" -> {
                            val filteredReviews = if (selectedUser != null && selectedLocation != null) {
                                reviews.filter { it.user == selectedUser!!.id && it.parking_location == selectedLocation!!.id }
                            } else emptyList()

                            Column {
                                // User filter dropdown
                                Text("Filter by User", style = MaterialTheme.typography.subtitle1)
                                DropdownSelector(
                                    options = users,
                                    selected = selectedUser,
                                    onSelect = { selectedUser = it },
                                    displayText = { it.name ?: "Unknown" }
                                )
                                Spacer(Modifier.height(16.dp))

                                // Location filter dropdown
                                Text("Filter by Location", style = MaterialTheme.typography.subtitle1)
                                DropdownSelector(
                                    options = locations,
                                    selected = selectedLocation,
                                    onSelect = { selectedLocation = it },
                                    displayText = { it.name }
                                )
                                Spacer(Modifier.height(16.dp))

                                Text("Reviews (${filteredReviews.size}):", style = MaterialTheme.typography.h6)
                                Spacer(Modifier.height(8.dp))

                                if (filteredReviews.isEmpty()) {
                                    Text("No reviews for selected filters.")
                                } else {
                                    LazyColumn {
                                        items(filteredReviews) { review ->
                                            ReviewItem(review)
                                            Spacer(Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                        "About" -> {
                            Text("Review Admin UI\nVersion 1.0\nDesigned similarly to Parking Location Admin.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewItem(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Rating: ${review.rating}", style = MaterialTheme.typography.subtitle1)
            review.review_text?.let {
                Text("Text: $it")
            }
            review.review_date?.let {
                Text("Date: ${it.toLocalDate()}")
            }
        }
    }
}



@Composable
fun PaymentAdminUI(
    users: List<User>,
    parkingLocations: List<ParkingLocation>,
    tariffs: List<Tariff>,
    payments: List<Payment>,
    onAddPayment: (Payment) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Add payment") }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var selectedLocation by remember { mutableStateOf<ParkingLocation?>(null) }
    var duration by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Credit Card") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val currentDateTime = remember { LocalDateTime.now() }

    MaterialTheme {
        Column {
            TopAppBar(
                title = { Text("Payment Admin") },
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
                    NavigationButton("Add payment") { selectedTab = it }
                    NavigationButton("View payments") { selectedTab = it }
                    Spacer(Modifier.weight(1f))
                    NavigationButton("About") { selectedTab = it }
                }

                Box(Modifier.fillMaxSize().padding(16.dp)) {
                    when (selectedTab) {
                        "Add payment" -> {
                            Column {
                                Text("Select User", style = MaterialTheme.typography.subtitle1)
                                DropdownSelector(
                                    options = users,
                                    selected = selectedUser,
                                    onSelect = { selectedUser = it },
                                    displayText = { it.name ?: "Unknown" }
                                )
                                Spacer(Modifier.height(16.dp))

                                Text("Select Parking Location", style = MaterialTheme.typography.subtitle1)
                                DropdownSelector(
                                    options = parkingLocations,
                                    selected = selectedLocation,
                                    onSelect = { selectedLocation = it },
                                    displayText = { it.name }
                                )
                                Spacer(Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = duration,
                                    onValueChange = { duration = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Duration (hours)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Spacer(Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = paymentMethod,
                                    onValueChange = { paymentMethod = it },
                                    label = { Text("Payment Method") }
                                )
                                Spacer(Modifier.height(16.dp))

                                val applicableTariffs = tariffs.filter { tariff ->
                                    selectedLocation?.id == tariff.parking_location
                                }
                                val tariffPricePerHour = applicableTariffs.firstOrNull()?.price ?: BigDecimal.ZERO

                                val amount = try {
                                    val dur = duration.toInt()
                                    tariffPricePerHour.multiply(BigDecimal(dur))
                                } catch (e: Exception) {
                                    BigDecimal.ZERO
                                }
                                Text("Amount: $amount")
                                Spacer(Modifier.height(16.dp))

                                errorMessage?.let {
                                    Text(it, color = MaterialTheme.colors.error)
                                    Spacer(Modifier.height(12.dp))
                                }

                                Button(
                                    onClick = {
                                        errorMessage = null
                                        if (selectedUser == null) {
                                            errorMessage = "Please select a User"
                                            return@Button
                                        }
                                        if (selectedLocation == null) {
                                            errorMessage = "Please select a Parking Location"
                                            return@Button
                                        }
                                        if (duration.isBlank() || duration.toIntOrNull() == null || duration.toInt() <= 0) {
                                            errorMessage = "Please enter a valid Duration"
                                            return@Button
                                        }
                                        val payment = Payment(
                                            date = currentDateTime,
                                            amount = amount,
                                            method = paymentMethod,
                                            payment_status = "pending",
                                            duration = duration.toInt(),
                                            user = selectedUser!!.id,
                                            parking_location = selectedLocation!!.id,
                                            created = currentDateTime,
                                            modified = currentDateTime
                                        )
                                        onAddPayment(payment)
                                        selectedUser = null
                                        selectedLocation = null
                                        duration = ""
                                        paymentMethod = "Credit Card"
                                    },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00796B)),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Save Payment", color = Color.White)
                                }
                            }
                        }

                        "View payments" -> {
                            val filteredPayments = if (selectedUser != null && selectedLocation != null) {
                                payments.filter { it.user == selectedUser!!.id && it.parking_location == selectedLocation!!.id }
                            } else emptyList()

                            Column {
                                Text("Filter by User", style = MaterialTheme.typography.subtitle1)
                                DropdownSelector(
                                    options = users,
                                    selected = selectedUser,
                                    onSelect = { selectedUser = it },
                                    displayText = { it.name ?: "Unknown" }
                                )
                                Spacer(Modifier.height(16.dp))
                                Text("Filter by Location", style = MaterialTheme.typography.subtitle1)
                                DropdownSelector(
                                    options = parkingLocations,
                                    selected = selectedLocation,
                                    onSelect = { selectedLocation = it },
                                    displayText = { it.name }
                                )
                                Spacer(Modifier.height(16.dp))

                                Text("Payments (${filteredPayments.size}):", style = MaterialTheme.typography.h6)
                                Spacer(Modifier.height(8.dp))

                                if (filteredPayments.isEmpty()) {
                                    Text("No payments for selected filters.")
                                } else {
                                    LazyColumn {
                                        items(filteredPayments) { payment ->
                                            PaymentItem(payment, users, parkingLocations)
                                            Spacer(Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }

                        "About" -> {
                            Text("Payment Admin UI\nVersion 1.0\nStyled similarly to Review Admin.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentItem(
    payment: Payment,
    users: List<User>,
    locations: List<ParkingLocation>
) {
    val userName = users.find { it.id == payment.user }?.name ?: "Unknown user"
    val locationName = locations.find { it.id == payment.parking_location }?.name ?: "Unknown location"
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("User: $userName", style = MaterialTheme.typography.subtitle1)
            Text("Location: $locationName")
            Text("Amount: ${payment.amount}")
            Text("Duration: ${payment.duration} hours")
            Text("Payment Method: ${payment.method}")
            Text("Status: ${payment.payment_status}")
            payment.date?.let {
                Text("Date: ${it.toLocalDate()}")
            }
        }
    }
}



fun main() = application {

    Window(onCloseRequest = ::exitApplication, title = "Compose Database Admin") {
        App()
    }



/*
    println("==========SUBSCRIBERS TESTS==========")
    val subscribersTest = SubscribersRepository()

    // 1. Kreiraj Subscriber
    val subscribers = Subscriber(
        available_spots = 5,
        reserved_spots = 3,
        total_spots = 10,
        waiting_line = 2,
        created = LocalDateTime.now(),
        modified = LocalDateTime.now(),
        hidden = false
    )

    // Ubaci u bazu
    subscribersTest.insert(subscribers)
    println("Ubacio subscriber sa ID: ${subscribers.id}")

    // 2. Dohvati po ID-u
    val fetche = subscribersTest.findById(subscribers.id.toHexString())
    println("Dohvatio subscriber: $fetche")

    // 3. Update subscriber - npr. promeni reserved_spots
    if (fetche != null) {
        val updatedSubscriber = fetche.copy(
            reserved_spots = 4,
            modified = LocalDateTime.now()
        )
        val updateResult = subscribersTest.update(updatedSubscriber)
        println("Update uspešan? $updateResult")
    }

    // 4. Dohvati ponovo da vidiš update
    val updatedFetched = subscribersTest.findById(subscribers.id.toHexString())
    println("Dohvatio nakon update-a: $updatedFetched")

    // 5. Soft delete
    val deleteResult = subscribersTest.deleteById(subscribers.id.toHexString())
    println("Soft delete uspešan? $deleteResult")

    // 6. Pokušaj dohvatiti obrisanog (trebalo bi da postoji ali sa hidden=true)
    val afterDelet = subscribersTest.findById(subscribers.id.toHexString())
    println("Dohvatio nakon soft delete: $afterDelet")

    // 7. Dohvati sve vidljive (hidden != true)
    val visibleSubscribers = subscribersTest.findAllVisible()
    println("Svi vidljivi subscriberi: $visibleSubscribers")





    println("==========PARKING LOCATION TESTS==========")
    val parkingLocationRepository = ParkingLocationRepository()

    // Kreiraj LocationCoordinates
    val locationCoordinates = LocationCoordinates(
        coordinates = listOf(15.966568, 45.815399)
    )

    // Kreiraj ParkingLocation
    val now = Date()
    val testLocation = ParkingLocation(
        id = ObjectId(), // ObjectId automatski kreiran
        name = "Test Parking",
        address = "Main Street 123",
        location = locationCoordinates,
        total_regular_spots = 100,
        total_invalid_spots = 5,
        total_bus_spots = 3,
        available_regular_spots = 90,
        available_invalid_spots = 5,
        available_bus_spots = 2,
        created = LocalDateTime.now(),
        modified = LocalDateTime.now(),
        description = "Parking in center of city",
        hidden = false
    )

    // Validacija
    if (testLocation.isValid()) {
        // Ubaci ParkingLocation
        parkingLocationRepository.insert(testLocation)

        // Prikaz svih parking lokacija
        val allLocations = parkingLocationRepository.findAll()
        println("✅ Svi ParkingLocations: $allLocations")

        // Ažuriraj parkingLocation
        val updatedLocation = testLocation.copy(
            available_regular_spots = 80,
            modified = LocalDateTime.now() // novo vreme
        )
        val updateResult = parkingLocationRepository.update(updatedLocation)
        println("✅ Update uspešan? $updateResult")
        println("ID: ${updatedLocation.id}")

        // Dohvati po ID-u
        val fetchedLocation = parkingLocationRepository.findById(testLocation.id.toHexString()) // važno!
        println("✅ Fetched by ID: $fetchedLocation")

        // Obrisi parkingLocation
        val deleteResult = parkingLocationRepository.deleteById(testLocation.id.toHexString()) // važno!
        println("✅ Delete uspešan? $deleteResult")
    } else {
        println("❌ ParkingLocation nije validan!")
    }




    println("==========PAYMENT TESTS==========")
    val paymentRepo = PaymentRepository()

    val userId = ObjectId() // testni user
    val parkingLocationId = ObjectId() // testni parking location

    val payment = Payment(
        amount = BigDecimal("9.99"),
        method = "card",
        payment_status = "pending",
        duration = 30,
        created = LocalDateTime.now(),
        modified = LocalDateTime.now(),
        hidden = false,
        user = userId,
        parking_location = parkingLocationId
    )

    // Validacija i insert
    if (payment.isValid()) {
        paymentRepo.insert(payment)

        // Dohvati sve
        val allPayments = paymentRepo.findAll()
        println("✅ Svi Payments: $allPayments")

        // Dohvati po ID-u
        val fetchedById = paymentRepo.findById(payment.id.toHexString())
        println("✅ Fetched by ID: $fetchedById")

        // Dohvati po statusu
        val pendingPayments = paymentRepo.findByStatus("pending")
        println("✅ Pending Payments: $pendingPayments")

        // Dohvati po user-u
        val byUser = paymentRepo.findByUser(userId.toHexString())
        println("✅ Payments by User: $byUser")

        // Dohvati po lokaciji
        val byLocation = paymentRepo.findByParkingLocation(parkingLocationId.toHexString())
        println("✅ Payments by Location: $byLocation")

        // Update
        val updatedPayment = payment.copy(payment_status = "completed", modified = LocalDateTime.now())
        val updated = paymentRepo.update(updatedPayment)
        println("✅ Update uspešan? $updated")

        // Brisanje
        val deleted = paymentRepo.deleteById(payment.id.toHexString())
        println("✅ Delete uspešan? $deleted")
    } else {
        println("❌ Payment nije validan!")
    }




    println("==========USER TESTS==========")
    val userRepository = UserRepository()

    // Kreiraj testnog user-a
    val testUser = User(
        name = "Milan",
        surname = "Jovanovic",
        username = "milan.jovanovic",
        email = "milan@example.com",
        password_hash = "Passw0rd@2025", // Za test
        phone_number = "381641234567",
        credit_card_number = "1234567890123456",
        user_type = "user",
        hidden = false,
        created_at = LocalDateTime.now(),
        updated_at = LocalDateTime.now(),
        vehicles = emptyList()
    )

    // Validacija
    val isValid = testUser.isUsernameValid() &&
            testUser.isEmailValid() &&
            testUser.isPasswordValid() &&
            testUser.isPhoneNumberValid() &&
            testUser.isCreditCardValid() &&
            testUser.isUserTypeValid()

    if (isValid) {
        // Ubaci user-a
        userRepository.insert(testUser)

        // Prikaži sve user-e
        val allUsers = userRepository.findAll()
        println("✅ Svi User-i: $allUsers")

        // Ažuriraj user-a
        val updatedUser = testUser.copy(
            phone_number = "381651234567",
            updated_at = LocalDateTime.now()
        )
        val updateResult = userRepository.update(updatedUser)
        println("✅ Update uspešan? $updateResult")

        // Pronađi po username-u
        val fetchedByUsername = userRepository.findByUsername("milan.jovanovic")
        println("✅ Fetched by username: $fetchedByUsername")

        // Pronađi po email-u
        val fetchedByEmail = userRepository.findByEmail("milan@example.com")
        println("✅ Fetched by email: $fetchedByEmail")

        // Pronađi po user_type
        val fetchedByUserType = userRepository.findByUserType("user")
        println("✅ Users by user_type: $fetchedByUserType")

        // Pronađi vidljive user-e
        val visibleUsers = userRepository.findVisible()
        println("✅ Vidljivi User-i: $visibleUsers")

        // Dohvati po ID-u
        val fetchedById = userRepository.findById(testUser.id.toHexString())
        println("✅ Fetched by ID: $fetchedById")

        // Obriši user-a
        val deleteResult = userRepository.deleteById(testUser.id.toHexString())
        println("✅ Delete uspešan? $deleteResult")
    } else {
        println("❌ Test user nije validan!")
    }




    println("==========VEHICLE TESTS==========")
    val vehicleRepo = VehicleRepository()

    // 1️⃣ Kreiraj testno vozilo
    //val userId = testUser.id // Pretpostavimo da je userId ObjectId od postojećeg korisnika
    val vehicle = Vehicle(
        registration_number = "ZG1234AB",
        vehicle_type = "car",
        created = LocalDateTime.now(),
        modified = LocalDateTime.now(),
        user = testUser.id,
        hidden = false
    )

    // 2️⃣ Validacija
    if (vehicle.isValid()) {
        println("✅ Vozilo je validno!")

        // 3️⃣ Ubacivanje vozila
        vehicleRepo.insert(vehicle)

        // 4️⃣ Dohvati po ID-u
        val fetched = vehicleRepo.findById(vehicle.id.toHexString())
        println("✅ Dohvaceno vozilo: $fetched")

        // 5️⃣ Ažuriranje vozila (npr. promena tipa)
        val updatedVehicle = vehicle.copy(
            vehicle_type = "truck",
            modified = LocalDateTime.now()
        )
        val updateResult = vehicleRepo.update(updatedVehicle)
        println("✅ Update uspešan? $updateResult")

        // 6️⃣ Dohvati sva vozila
        val allVehicles = vehicleRepo.findAll()
        println("✅ Sva vozila: $allVehicles")

        // 7️⃣ Dohvati vozila po tipu
        val trucks = vehicleRepo.findByVehicleType("truck")
        println("✅ Vozila tipa 'truck': $trucks")

        // 8️⃣ Dohvati vozila po korisniku
        val userVehicles = vehicleRepo.findByUser(userId.toHexString())
        println("✅ Vozila za korisnika $userId: $userVehicles")

        // 9️⃣ Dohvati po registracionom broju
        val byRegNumber = vehicleRepo.findByRegistrationNumber("ZG1234AB")
        println("✅ Vozilo po registracionom broju: $byRegNumber")

        // 🔟 Obrisi vozilo
        //val deleteResult = vehicleRepo.deleteById(vehicle.id.toHexString())
        //println("✅ Brisanje uspešno? $deleteResult")
    } else {
        println("❌ Vozilo nije validno!")
    }




    println("==========UPDATED USER==========")
// Ažuriraj user-a (dodaj vozilo u vehicles listu)
    val updatedUser = testUser.copy(
        vehicles = listOf(vehicle),
        updated_at = LocalDateTime.now()
    )
    val updateResult = userRepository.update(updatedUser)
    println("✅ Update uspešan? $updateResult")

// Dohvati vozila za user-a (koristi testUser.id)
    val userVehicles = vehicleRepo.findByUser(testUser.id.toHexString())
    println("✅ Vozila za korisnika ${testUser.id}: $userVehicles")




    println("==========REVIEW TEST==========")
    val repo = ReviewsRepository()

    // Kreiraj novi review
    val newReview = Review(
        rating = 4,
        review_text = "Dobar parking, pristojna cena.",
        review_date = LocalDateTime.now().minusDays(1),
        hidden = false,
        created = LocalDateTime.now().minusDays(2),
        modified = LocalDateTime.now().minusDays(1),
        user = testUser.id,
        parking_location = testLocation.id
    )

    // Ubaci review u bazu
    repo.insert(newReview)

    // Dohvati po ID-u
    val fetched = repo.findById(newReview.id.toHexString())
    println("Dohvacen review: $fetched")

    // Ažuriraj review (npr. promeni rating)
    val updatedReview = fetched?.copy(rating = 5, modified = LocalDateTime.now())
    if (updatedReview != null) {
        val success = repo.update(updatedReview)
        println("Update uspešan? $success")

        // Dohvati opet i proveri update
        val updatedFetched = repo.findById(updatedReview.id.toHexString())
        println("Ažurirani review: $updatedFetched")
    }

    // Brisanje (soft delete)
    val deleted = repo.deleteById(newReview.id.toHexString())
    println("Soft delete uspešan? $deleted")

    // Pokušaj da dohvatiš review posle brisanja
    val afterDelete = repo.findById(newReview.id.toHexString())
    println("Review nakon brisanja (trebao bi biti sa hidden=true): $afterDelete")

    // Dohvati sve visible review-e
    val visibleReviews = repo.findAllVisible()
    println("Visible reviews count: ${visibleReviews.size}")




    println("==========TARIFF TESTS==========")
    val rep = TariffRepository()

    val tariff = Tariff(
        tariff_type = "Standard",
        duration = "08:00-18:00",
        vehicle_type = "Automobil",
        price = BigDecimal("10.00"),
        price_unit = "ura",
        created = LocalDateTime.now(),
        modified = LocalDateTime.now(),
        parking_location = testLocation.id
    )

    if (tariff.isValid()) {
        rep.insert(tariff)
    } else {
        println("❌ Tariff nije validan!")
    }

    // 2️⃣ Dohvati sve Tariffe
    println("\n📄 Svi Tariffi:")
    val allTariffs = rep.findAll()
    allTariffs.forEach { println(it) }

    // 3️⃣ Dohvati po ID-u
    val fetc = rep.findById(tariff.id.toHexString())
    println("\n🔎 Dohvacen po ID-u: $fetc")

    // 4️⃣ Azuriraj Tariff
    val updatedTariff = fetc?.copy(
        price = BigDecimal("12.50"),
        modified = LocalDateTime.now()
    )
    if (updatedTariff != null) {
        val updated = rep.update(updatedTariff)
        println("✏️ Azuriran: $updated")
    }

    // 5️⃣ Soft delete
    val del = rep.deleteById(tariff.id.toHexString())
    println("\n🗑️ Soft delete uspesan? $del")

    // 6️⃣ Dohvati sve vidljive Tariffe
    println("\n📄 Svi vidljivi Tariffi:")
    val visibleTariffs = rep.findAllVisible()
    visibleTariffs.forEach { println(it) }

    // 7️⃣ Dohvati po parking lokaciji
    println("\n🅿️ Tariffi za parking lokaciju: $testLocation.id")
    val tariffsByLocation = rep.findByParkingLocation(testLocation.id.toHexString())
    tariffsByLocation.forEach { println(it) }

    println("\n✅ Testiranje gotovo.")
    */
}
