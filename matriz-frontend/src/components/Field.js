function Field({
  label,
  value,
  onChange,
  type = "text",
  options,
  rows,
  ...rest
}) {
  return (
    <label className="field">
      <span>{label}</span>
      {options ? (
        <select
          value={value}
          onChange={(e) => onChange(e.target.value)}
          {...rest}
        >
          {options.map(([key, text]) => (
            <option key={key} value={key}>
              {text}
            </option>
          ))}
        </select>
      ) : rows ? (
        <textarea
          value={value}
          onChange={(e) => onChange(e.target.value)}
          rows={rows}
          {...rest}
        />
      ) : (
        <input
          type={type}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          {...rest}
        />
      )}
    </label>
  );
}
export default Field;
