const changeLogSchema = new Schema({
  changed_table_name: String,
  record_id: Number,
  type_of_change: String,
  time_of_change: Date
});

module.exports = mongoose.model('ChangeLog', changeLogSchema);
