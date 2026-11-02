function Badge({ value }) {
  return (
    <span className={`badge ${value?.toLowerCase()}`}>
      {value?.replace("_", " ")}
    </span>
  );
}
export default Badge;
